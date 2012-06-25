/*
  Copyright (c) 2008, Adobe Systems Incorporated
  All rights reserved.

  Redistribution and use in source and binary forms, with or without 
  modification, are permitted provided that the following conditions are
  met:

  * Redistributions of source code must retain the above copyright notice, 
    this list of conditions and the following disclaimer.
  
  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the 
    documentation and/or other materials provided with the distribution.
  
  * Neither the name of Adobe Systems Incorporated nor the names of its 
    contributors may be used to endorse or promote products derived from 
    this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package os.json;

public class JsonTokenizer
{
	
	
	
	private String jsonString;
	private int loc;
	private char ch;
	
	//private const controlCharsRegExp:RegExp = /[\x00-\x1F]/;
	
	/**
	 * Constructs a new JSONDecoder to parse a JSON string
	 * into a native object.
	 *
	 * @param s The JSON string to be converted
	 *		into a native object
	 */
	public JsonTokenizer( String s) {
		jsonString = s;
		loc = 0;
		nextChar();
	}
	
	/**
	 * Gets the next token in the input sting and advances
	 * the character to the next character after the token
	 * @throws JsonParseError 
	 */
	public JsonToken getNextToken() throws JsonParseError
	{
		
		JsonToken token = null;
		
		// skip any whitespace / comments since the last 
		// token was read
		skipIgnored();
		
		// examine the new character and see what we have...
		switch ( ch )
		{
			case '{':
				token = JsonToken.create( JsonToken.Type.LEFT_BRACE, ch );
				nextChar();
			break;
			case '}':
				token = JsonToken.create( JsonToken.Type.RIGHT_BRACE, ch );
				nextChar();
			break;
			case '[':
				token = JsonToken.create( JsonToken.Type.LEFT_BRACKET, ch );
				nextChar();
			break;
			case ']':
				token = JsonToken.create( JsonToken.Type.RIGHT_BRACKET, ch );
				nextChar();
			break;
			case ',':
				token = JsonToken.create( JsonToken.Type.COMMA, ch );
				nextChar();
			break;
			case ':':
				token = JsonToken.create( JsonToken.Type.COLON, ch );
				nextChar();
			break;
			case 't': // attempt to read true
				String possibleTrue = "t" + nextChar() + nextChar() + nextChar();
				if ( possibleTrue == "true" ){
					token = JsonToken.create( JsonToken.Type.TRUE, true );
					nextChar();
				}else{
					parseError( "Expecting 'true' but found " + possibleTrue );
				}
			break;
			case 'f': // attempt to read false
				String possibleFalse = "f" + nextChar() + nextChar() + nextChar() + nextChar();
				
				if ( possibleFalse == "false" )
				{
					token = JsonToken.create( JsonToken.Type.FALSE, false );
					nextChar();
				}
				else
				{
					parseError( "Expecting 'false' but found " + possibleFalse );
				}
			break;
			case 'n': // attempt to read null
				String possibleNull = "n" + nextChar() + nextChar() + nextChar();
				if ( possibleNull == "null" ){
					token = JsonToken.create( JsonToken.Type.NULL, null );
					nextChar();
				}else{
					parseError( "Expecting 'null' but found " + possibleNull );
				}
				break;
			case '"': // the start of a string
				token = readString();
			break;
			default:
				// see if we can read a number
				if ( isDigit( ch ) || ch == '-' ){
					token = readNumber();
				}
				else if ( ch == 0 ){
					// check for reading past the end of the string
					token = null;
				}else{
					// not sure what was in the input string - it's not
					// anything we expected
					parseError( "Unexpected " + ch + " encountered" );
				}
		}
		return token;
	}

	private JsonToken readString() throws JsonParseError{
		// Rather than examine the string character-by-character, it's
		// faster to use indexOf to try to and find the closing quote character
		// and then replace escape sequences after the fact.
		
		// Start at the current input stream position
		
		int quoteIndex = loc;
		do
		{
			// Find the next quote in the input stream
			quoteIndex = jsonString.indexOf( "\"", quoteIndex );
			
			if ( quoteIndex >= 0 )
			{
				// We found the next double quote character in the string, but we need
				// to make sure it is not part of an escape sequence.
				
				// Keep looping backwards while the previous character is a backslash
				int backspaceCount = 0;
				int backspaceIndex = quoteIndex - 1;
				while ( jsonString.charAt( backspaceIndex ) == '\\' )
				{
					backspaceCount++;
					backspaceIndex--;
				}
				
				// If we have an even number of backslashes, that means this is the ending quote 
				if ( ( backspaceCount & 1 ) == 0 )
				{
					break;
				}
				
				// At this point, the quote was determined to be part of an escape sequence
				// so we need to move past the quote index to look for the next one
				quoteIndex++;
			}
			else // There are no more quotes in the string and we haven't found the end yet
			{
				parseError( "Unterminated string literal" );
			}
		} while ( true );
		
		// Unescape the string
		// the token for the string we'll try to read
		JsonToken token = JsonToken.create( 
				JsonToken.Type.STRING,
				// Attach resulting string to the token to return it
				unescapeString( jsonString.substring( loc, quoteIndex) ) );
		
		// Move past the closing quote in the input string.  This updates the next
		// character in the input stream to be the character one after the closing quote
		loc = quoteIndex + 1;
		nextChar();
		
		return token;
	}
	
	public String unescapeString( String input ) throws JsonParseError{
		// Issue #104 - If the string contains any unescaped control characters, this
		// is an error in strict mode
		
		/*if ( controlCharsRegExp.test( input ) )
		{
			parseError( "String contains unescaped control character (0x00-0x1F)" );
		}*/
		
		String result = "";
		int backslashIndex = 0;
		int nextSubstringStartPosition = 0;
		int len = input.length();
		do
		{
			// Find the next backslash in the input
			backslashIndex = input.indexOf( '\\', nextSubstringStartPosition );
			
			if ( backslashIndex >= 0 )
			{
				result += input.substring( nextSubstringStartPosition, backslashIndex );
				
				// Move past the backslash and next character (all escape sequences are
				// two characters, except for \\u, which will advance this further)
				nextSubstringStartPosition = backslashIndex + 2;
				
				// Check the next character so we know what to escape
				char escapedChar = input.charAt( backslashIndex + 1 );
				switch ( escapedChar )
				{
					// Try to list the most common expected cases first to improve performance
					
					case '"':
						result += escapedChar;
						break; // quotation mark
					case '\\':
						result += escapedChar;
						break; // reverse solidus	
					case 'n':
						result += '\n';
						break; // newline
					case 'r':
						result += '\r';
						break; // carriage return
					case 't':
						result += '\t';
						break; // horizontal tab	
					
					// Convert a unicode escape sequence to it's character value
					case 'u':
						
						// Save the characters as a string we'll convert to an int
						String hexValue = "";
						
						int unicodeEndPosition = nextSubstringStartPosition + 4;
						
						// Make sure there are enough characters in the string leftover
						if ( unicodeEndPosition > len )
						{
							parseError( "Unexpected end of input.  Expecting 4 hex digits after \\u." );
						}
						
						// Try to find 4 hex characters
						for ( int i = nextSubstringStartPosition; i < unicodeEndPosition; i++ )
						{
							// get the next character and determine
							// if it's a valid hex digit or not
							char possibleHexChar = input.charAt( i );
							if ( !isHexDigit( possibleHexChar ) )
							{
								parseError( "Excepted a hex digit, but found: " + possibleHexChar );
							}
							
							// Valid hex digit, add it to the value
							hexValue += possibleHexChar;
						}
						
						// Convert hexValue to an integer, and use that
						// integer value to create a character to add
						// to our string.
						result += String.valueOf(Integer.parseInt(hexValue, 16 ));
						
						// Move past the 4 hex digits that we just read
						nextSubstringStartPosition = unicodeEndPosition;
						break;
					
					case 'f':
						result += '\f';
						break; // form feed
					case '/':
						result += '/';
						break; // solidus
					case 'b':
						result += '\b';
						break; // bell
					default:
						result += '\\' + escapedChar; // Couldn't unescape the sequence, so just pass it through
				}
			}
			else
			{
				// No more backslashes to replace, append the rest of the string
				result += input.substring( nextSubstringStartPosition );
				break;
			}
			
		} while ( nextSubstringStartPosition < len );
		
		return result;
	}
	
	/**
	 * Attempts to read a number from the input string.  Places
	 * the character location at the first character after the
	 * number.
	 *
	 * @return The JSONToken with the number value if a number could
	 * 		be read.  Throws an error otherwise.
	 * @throws JsonParseError 
	 */
	private final JsonToken readNumber() throws JsonParseError {
		
		// the string to accumulate the number characters
		// into that we'll convert to a number at the end
		String input = "";
		
		// check for a negative number
		if ( ch == '-' )
		{
			input += '-';
			nextChar();
		}
		
		// the number must start with a digit
		if ( !isDigit( ch ) )
		{
			parseError( "Expecting a digit" );
		}
		
		// 0 can only be the first digit if it
		// is followed by a decimal point
		if ( ch == '0' )
		{
			input += ch;
			nextChar();
			
			// make sure no other digits come after 0
			if ( isDigit( ch ) )
			{
				parseError( "A digit cannot immediately follow 0" );
			}
		}
		else
		{
			// read numbers while we can
			while ( isDigit( ch ) )
			{
				input += ch;
				nextChar();
			}
		}
		
		// check for a decimal value
		if ( ch == '.' )
		{
			input += '.';
			nextChar();
			
			// after the decimal there has to be a digit
			if ( !isDigit( ch ) )
			{
				parseError( "Expecting a digit" );
			}
			
			// read more numbers to get the decimal value
			while ( isDigit( ch ) )
			{
				input += ch;
				nextChar();
			}
		}
		
		// check for scientific notation
		if ( ch == 'e' || ch == 'E' )
		{
			input += "e";
			nextChar();
			// check for sign
			if ( ch == '+' || ch == '-' )
			{
				input += ch;
				nextChar();
			}
			
			// require at least one number for the exponent
			// in this case
			if ( !isDigit( ch ) )
			{
				parseError( "Scientific notation number needs exponent value" );
			}
			
			// read in the exponent
			while ( isDigit( ch ) )
			{
				input += ch;
				nextChar();
			}
		}
		return JsonToken.create( JsonToken.Type.NUMBER, input );
	}
	
	private char nextChar() {
		try{
			return ch = jsonString.charAt( loc++ );
		}catch(IndexOutOfBoundsException ex){
			return ch = 0;
		}
	}
	
	private void skipIgnored() throws JsonParseError {
		int originalLoc;
		do {
			originalLoc = loc;
			skipWhite();
			skipComments();
		} while ( originalLoc != loc );
	}
	
	private void skipComments() throws JsonParseError {
		if ( ch == '/' ) {
			// Advance past the first / to find out what type of comment
			nextChar();
			switch ( ch )
			{
				case '/': // single-line comment, read through end of line
					
					// Loop over the characters until we find
					// a newline or until there's no more characters left
					do
					{
						nextChar();
					} while ( ch != '\n' && ch != 0 );
					
					// move past the \n
					nextChar();
					
					break;
				
				case '*': // multi-line comment, read until closing */
					
					// move past the opening *
					nextChar();
					
					// try to find a trailing */
					while ( true )
					{
						if ( ch == '*' )
						{
							// check to see if we have a closing /
							nextChar();
							if ( ch == '/' )
							{
								// move past the end of the closing */
								nextChar();
								break;
							}
						}
						else
						{
							// move along, looking if the next character is a *
							nextChar();
						}
						
						// when we're here we've read past the end of 
						// the string without finding a closing */, so error
						if ( ch == 0 )
						{
							parseError( "Multi-line comment not closed" );
						}
					}
					
					break;
				
				// Can't match a comment after a /, so it's a parsing error
				default:
					parseError( "Unexpected " + ch + " encountered (expecting '/' or '*' )" );
			}
		}
	
	}
	
	private void skipWhite(){
		while (isWhiteSpace(ch)){
			nextChar();
		}
	}
	
	private Boolean isWhiteSpace(char ch) {
		if ( ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' ){
			return true;
		}
		return false;
	}
	
	private Boolean isDigit( char ch){
		return ( ch >= '0' && ch <= '9' );
	}
	
	private Boolean isHexDigit( char ch ) {
		return ( isDigit( ch ) || ( ch >= 'A' && ch <= 'F' ) || ( ch >= 'a' && ch <= 'f' ) );
	}
	
	public void parseError(String message) throws JsonParseError{
		throw new JsonParseError( message, loc, jsonString );
	}

	public String getObjectString() {
		int d=1;
		String str = "";
		switch(jsonString.charAt(loc-1)){
			case '{': {
				while(d>0){
					if(ch==0){
						break;
					}else
					if(ch=='}'){
						d--;
					}else 
					if(ch=='{'){
						d++;
					}
					str+=ch;
					nextChar();	
				}
				break;
			}
			case '[': {
				while(d>0){
					if(ch==0){
						break;
					}else
					if(ch==']'){
						d--;
					}else 
					if(ch=='['){
						d++;
					}
					str+=ch;
					nextChar();	
				}
				break;
			}
		}
		return str;
	}
}


