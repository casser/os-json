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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class JSON {

	
	public static <T> T decode(String document) throws JsonParseError{
		return decode(document,null);
	}
	
	public static <T> T decode(File file) throws java.io.IOException, JsonParseError{
		return decode(file,null);
	}
	
	public static <T> T decode(File file, Class<T> type) throws java.io.IOException, JsonParseError{
	    byte[] buffer = new byte[(int) file.length()];
	    BufferedInputStream f = null;
	    try {
	        f = new BufferedInputStream(new FileInputStream(file));
	        f.read(buffer);
	    } finally {
	        if (f != null) try { f.close(); } catch (IOException ignored) { }
	    }
	    return decode(new String(buffer),type);
	}
	
	public static <T> T decode(String document, Class<T> type) throws JsonParseError{
		return (T)(new JsonDecoder().decode(document,type));
	}
	
	public static String schema(Class<?> document){
		return schema(document,false);
	}
	public static String schema(Class<?> document, Boolean formated){
		return new JsonSchemaEncoder(formated).encode(document);
	}
	
	public static String encode(Object document){
		return encode(document,false,false);
	}
	public static String encode(Object document, Boolean formated){
		return encode(document,formated,false);
	}
	public static String encode(Object document, Boolean formated, Boolean commented){
		return new JsonEncoder(formated,commented).encode(document);
	}

	public static void print(Object obj) {
		System.out.println(encode(obj,true));
	}
	
}

