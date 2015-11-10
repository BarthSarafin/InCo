package Informationstheorie;
// ============================================================================
//
//      LZ77 Encoder and Decoder
//
// ============================================================================
//
//      Version      1.00
//      Date         2015-03-12
//      Author       J. M. Stettbacher
//
//      System       Java (tested with version 1.7.0_55 on Linux)
//
// ============================================================================
//
//      Build class file:
//      (1) Compile using Eclipse or other.
//      (2) Compile using Makefile. Type on command line:
//          >> make
//      (3) Compile directly on command line:
//          >> javac LZ77.java
//
//      Execute class file (filename is a valid data file in ASCII format):
//      (1) Run from Eclipse by specifying the filename command line argument.
//      (2) Run from command line:
//          >> java LZ77 filename
//
//      Description:
//      - checks if input file (from command line) exists.
//	- if so, open it and open encoder file, instead exit.
//	- read symbols from input file and feed into preview buffer.
//      - from preview buffer build LZ77 tokens and write to output file.
//      - tokens are in ASCII and decimal, one per line and elements are comma-separated.
//      - after encodeing start decoding.
//	- re-open encoder file and open decoder file.
//      - read tokens from encoder file and reconstruct original message.
//
//	This implementation only uses elementary array operations and does
//	not make use of any kind of higher-level functions.
//
//	Buffer arrangement:
//
//	      search buffer             preview buffer
//	+---+---+---+ ... -+---+   +---+---+---+ ... -+---+        input
//	| 0 | 1 | 2 |      |N-1|   | 0 | 1 | 2 |      |M-1|  <---  from
//	+---+---+---+ ... -+---+   +---+---+---+ ... -+---+        file
//
//	N: search buffer length (SEARCH_BUF_SIZE).
//	M: preview buffer length (PREVIEW_BUF_SIZE).
//
// ============================================================================


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


// ----------------------------------------------------------------------------
// Main class:
// ----------------------------------------------------------------------------
public class LZ77 {

	// Global counter variable:
	static final String  FILE_ENC  = "LZ77-encoded.txt";
	static final String  FILE_DEC  = "LZ77-decoded.txt";
	static final char    ZERO_CHAR = 0;
	static final Boolean DEBUG     = false;
	static final int     SEARCH_BUF_SIZE  = 10;
	static final int     PREVIEW_BUF_SIZE = 4;
	static int    CharactersCount = 0;
	static char[] search  = new char[SEARCH_BUF_SIZE];
	static char[] preview = new char[PREVIEW_BUF_SIZE];


	// Main method. The program starts here.
	public static void main(String[] args) {

		// Print hello message:
		System.out.println("======================================================");
		System.out.println("Starting LZ77 ...");

		// ------------------------------------------------------------
		// Check if a valid filename has been supplied on command line:
		// ------------------------------------------------------------
		// Is there a command line argument at all?
		if (args.length <= 0) {
			System.out.println("ERROR: You have to supply a filename on the command line!");
			System.out.println(" ");
			System.exit(0);
		}
		// Yes, there is one:
		String s = args[0];
		File file = new File(s);
		// Check if file with that name exists:
		if (!file.exists()) {
			// Quit program with an error message:
			System.out.println("ERROR: Data file: " + s + " does not exist!");
			System.out.println(" ");
			System.exit(0);
		}
		// Yes, command line argument is okay.
		System.out.println( "Data file " + s + " exists.");

		// ------------------------------------------------------------
		// Call encoder and decoder:
		// ------------------------------------------------------------
		System.out.println( "Starting LZ77 encoder.");
		Encode(s);

		System.out.println( "Starting LZ77 decoder.");
		Decode();

		// ------------------------------------------------------------
		// Print goodbye message:
		// ------------------------------------------------------------
		System.out.println("Done.");
		System.out.println("======================================================");
	}


	// --------------------------------------------------------------------
	// LZ77 token class:
	// --------------------------------------------------------------------
	static class Token {
		int   offset = 0;
		int   size   = 0;
		char  nextc  = ZERO_CHAR;
	}


	// --------------------------------------------------------------------
	// LZ77 encoder:
	// --------------------------------------------------------------------
	static void Encode(String in_file) {

		Token token = new Token();
		int   k, count = 0;

		// Open files:
		try (BufferedReader in = new BufferedReader(new FileReader(in_file));
			 BufferedWriter out = new BufferedWriter(new FileWriter(FILE_ENC))) {

			System.out.println( "--- Files opened.");

			// ----------------------------------------------------
			// Initialize preview and search buffers:
			// ----------------------------------------------------
			// Set search buffer to all ZERO_CHARs:
			// Note: we use zero for non-existing characters.
			// Note: zero-character never occur in ASCII strings.
			for (k = 0; k < SEARCH_BUF_SIZE;  k++) {
				search[k] = ZERO_CHAR;
			}

			// Initialize preview buffer with characters from in file:
			FillInPreviewBuffer(in, PREVIEW_BUF_SIZE);


			// ----------------------------------------------------
			// Build all tokens:
			// ----------------------------------------------------
			System.out.println( "--- Building tokens.");

			// Repeat until preview buffer is empty:
			// Note: the preview buffer is empty when a zero-character reaches its left end.
			while (preview[0] != ZERO_CHAR) {

				// Count tokens:
				count++;

				// Search for a copy of left side of preview buffer in search buffer:
				FindBestMatch(token);

				// Write token (in ASCII) to out file:
				out.write(String.valueOf(token.offset) + ","
						+ String.valueOf(token.size) + ","
						+ String.valueOf((int)token.nextc) + "\n");

				// Move tokenized characters from preview to search buffer:
				ShiftBuffers(in, token);
			}

			// That's it. Print out some statistics:
			System.out.println( "--- Tokens done.");
			System.out.println( "--- " + String.valueOf(CharactersCount) + " characters processed.");
			System.out.println( "--- " + String.valueOf(count) + " tokens generated.");
		}
		catch(IOException ioe) {}
	};


	// --------------------------------------------------------------------
	// LZ77 decoder:
	// --------------------------------------------------------------------
	static void Decode() {

		Token token = new Token();
		String   line;
		String[] elements;
		int      k, count = 0;

		// Open files:
		try (BufferedReader in = new BufferedReader(new FileReader(FILE_ENC));
			 BufferedWriter out = new BufferedWriter(new FileWriter(FILE_DEC))) {

			System.out.println( "--- Files opened.");

			// ----------------------------------------------------
			// Initialize search buffer:
			// ----------------------------------------------------
			// Set search buffer to all ZERO_CHAR:
			// Note: we use zero for non-existing characters.
			// Note: we don't need the preview buffer here.
			for (k = 0; k < SEARCH_BUF_SIZE;  k++) {
				search[k] = ZERO_CHAR;
			}

			// ----------------------------------------------------
			// Read and process all tokens:
			// ----------------------------------------------------
			System.out.println( "--- Processing tokens.");

			// Repeat until there are no more tokens in file:
			while (((line = in.readLine()) != null) && (line.length() > 2)) {

				// Count tokens:
				count++;

				// Split input-line into elements and build token:
				elements = line.split(",");
				token.offset = Integer.parseInt(elements[0]);
				token.size   = Integer.parseInt(elements[1]);
				token.nextc  = (char)Integer.parseInt(elements[2]);

				// Decode token and rebuild search buffer and write output:
				DecodeToken(token, out);
			}

			// That's it. Print some statistics:
			System.out.println( "--- Tokens done.");
			System.out.println( "--- " + String.valueOf(count) + " tokens processed.");
		}
		catch(IOException ioe) {}
	};


	// --------------------------------------------------------------------
	// Fill-in preview buffer from file.
	// --------------------------------------------------------------------
	static void FillInPreviewBuffer(BufferedReader in, int size) throws IOException {

		int k, c;

		// Move existing symbols from right to left through preview buffer:
		for (k = 0; k < PREVIEW_BUF_SIZE-size; k++) {
			preview[k] = preview[k+size];
		}

		// Insert new symbols (from file) at the right side of preview buffer:
		for (k = PREVIEW_BUF_SIZE-size; k < PREVIEW_BUF_SIZE; k++) {

			// Read characters from input file:
			if ((c = in.read()) == -1) {
				// Take a zero-character if there are no more symbols in file.
				c = ZERO_CHAR;
			} else {
				CharactersCount++;
			}
			preview[k] = (char)c;
		}
	}


	// --------------------------------------------------------------------
	// Find best match between preview and search buffer:
	// --------------------------------------------------------------------
	static void FindBestMatch(Token token) {

		int size, k;

		// Init token:
		token.offset = 0;
		token.size   = 0;
		token.nextc  = preview[0];

		// Parse from left to right through search buffer:
		for (k = 0; k < SEARCH_BUF_SIZE; k++) {

			// Test for match of first character in preview:
			if (search[k] == preview[0]) {

				// If we have a match, check if more characters match:
				size = 1;
				
				
				
				/*
				 * ToDo: [1.1] implement search of matching length.
				 */



				// Now we have a complet matching sequence.
				// Check if this is a new best match:
				if (size > token.size) {
					// If so, copy token:
					token.offset = SEARCH_BUF_SIZE-k;
					token.size   = size;
					token.nextc  = preview[size];

					// Make sure, this token does not contain a ZERO_CHAR:
					// Note: this may happen when the preview buffer gets empty.
					if (token.nextc == ZERO_CHAR) {
					
					
					
					/* 
					 * ToDo: [1.2] implement modification of token.
					 */



					}
				}
			}
		}
	}


	// --------------------------------------------------------------------
	// Shift tokenized characters from preview to search buffer:
	// --------------------------------------------------------------------
	static void ShiftBuffers(BufferedReader in, Token token) throws IOException {

		int k;

		
		
		/* 
		 * ToDo: [1.3] implement shift from preview to search buffer.
		 *	- Shift search buffer from right to left.
		 *	- Transfer tokenized characters from preview to search buffer.
		 *	- Refill preview buffer with characters from input file.
		 */


	}


	// --------------------------------------------------------------------
	// Decode token and rebuild search buffer:
	// --------------------------------------------------------------------
	static void DecodeToken(Token token, BufferedWriter out) throws IOException {

		char[] str = new char[token.size+1];
		int    k;
		
		
		
		/*
		 * ToDo: [2.1] implement decoding of token.
		 *	- Extract string described by token from search buffer and
		 *	  create a temporary array (str) from it.
		 *	- Shift search buffer to the left.
		 *	- Add temporary string to the right side of search buffer.
		 */



		out.write(str, 0, token.size+1);
	}
}

// ============================================================================
// ============================================================================