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

import java.util.List;
import java.util.Map;
import java.util.Set;

import os.utils.Types;

	
public class JsonDecoder {
	
	private Object value;
	private JsonTokenizer tokenizer;
	private JsonToken token;
	
	@SuppressWarnings("unchecked")
	public <T> T decode(String document, Class<T> type) throws JsonParseError{
		
		tokenizer = new JsonTokenizer(document);
		
		nextToken();
		value = parseValue(type);
		
		if (nextToken() != null ){
			tokenizer.parseError( "Unexpected characters left in input stream" );
		}
		return (T) value;
	}
	
	private JsonToken nextToken() throws JsonParseError {
		return token = tokenizer.getNextToken();
	}
	
	private JsonToken nextValidToken() throws JsonParseError {
		token = tokenizer.getNextToken();
		checkValidToken();
		return token;
	}
	
	private void checkValidToken() throws JsonParseError {
		if ( token == null ){
			tokenizer.parseError( "Unexpected end of input" );
		}
	}
	
	private <T> T parseArray(Class<T> cls) throws JsonParseError {
		Types.Type type = Types.getType(cls);
		T a = type.newInstance();
		
		// grab the next token from the tokenizer to move
		// past the opening [
		nextValidToken();
		
		// check to see if we have an empty array
		if ( token.type == JsonToken.Type.RIGHT_BRACKET )
		{
			// we're done reading the array, so return it
			return (T)a;
		}
		
		// deal with elements of the array, and use an "infinite"
		// loop because we could have any amount of elements
		while ( true )
		{
			// read in the value and add it to the array
			readValue(a);
			// after the value there should be a ] or a ,
			nextValidToken();
			
			if ( token.type == JsonToken.Type.RIGHT_BRACKET )
			{
				// we're done reading the array, so return it
				return (T)a;
			}
			else if ( token.type == JsonToken.Type.COMMA )
			{
				// move past the comma and read another value
				nextToken();
			}
			else
			{
				tokenizer.parseError( "Expecting ] or , but found " + token.value );
			}
		}
		
		
	}
	
	private Object convertKey(Object o, Class<?> cls) throws JsonParseError {
		Types.Type type = Types.getType(cls);
		if(type.isEnum()){
			return toEnum(o, type);
		}else
		if(type.isBean()){
			return toBean(o, type);
		}
		return o;
	}
	
	@SuppressWarnings("unchecked")
	private <T> T toBean(Object data, Types.Type type){
		return (T) type.newInstance(data);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T toEnum(Object data, Types.Type type){
		Object[] list = type.getType().getEnumConstants();
		for(Object item:list){
			Enum<?> en = (Enum<?>)item;
			if(en.name().toUpperCase().equals(data.toString().toUpperCase())){
				return (T) en;
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private void readValue(Object o) throws JsonParseError {
		Types.Type type = Types.getType(o.getClass());
		if(List.class.isAssignableFrom(o.getClass())){
			List<Object> list = (List<Object>)o;
			list.add(parseValue(type.getValueType()));
		}else
		if(Set.class.isAssignableFrom(o.getClass())){
			Set<Object> list = (Set<Object>)o;
			list.add(parseValue(type.getValueType()));
		}
	}
	
	@SuppressWarnings("unchecked")
	private void readValue(Object o, Object k) throws JsonParseError {
		Types.Type type = Types.getType(o.getClass());
		if(type.isMap()){
			Map<Object,Object> map = (Map<Object,Object>)o;
			map.put(convertKey(k,type.getKeyType()),parseValue(type.getValueType()));
		}else if(type.isBean()){
			if(type.getProperties().containsKey(k)){
				Types.Property property = type.getProperties().get(k);
				property.invokeSetter(o, parseValue(property.getType()));
			}
		}
	}
	
	
	
	/**
	 * Attempt to parse an object.
	 * @throws JsonParseError 
	 */
	@SuppressWarnings("unchecked")
	private <T> T parseObject(Class<T> cls) throws JsonParseError {
		if(cls!=null && JsonDecodable.class.isAssignableFrom(cls)){
			JsonDecodable value = null;
			try {
				value = (JsonDecodable) cls.newInstance();
				value.decodeJson(tokenizer.getObjectString());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return (T)value;
		}
		
		Types.Type type = Types.getType(cls);
		// create the object internally that we're going to
		// attempt to parse from the tokenizer
		T o = type.newInstance();
		
		// store the string part of an object member so
		// that we can assign it a value in the object
		String key;
		
		// grab the next token from the tokenizer
		nextValidToken();
		
		// check to see if we have an empty object
		if ( token.type == JsonToken.Type.RIGHT_BRACE )
		{
			// we're done reading the object, so return it
			return (T)o;
		}
		// in non-strict mode an empty object is also a comma
		// followed by a right bracket
		else if ( token.type == JsonToken.Type.COMMA )
		{
			// move past the comma
			nextValidToken();
			
			// check to see if we're reached the end of the object
			if ( token.type == JsonToken.Type.RIGHT_BRACE )
			{
				return (T)o;
			}
			else
			{
				tokenizer.parseError( "Leading commas are not supported.  Expecting '}' but found " + token.value );
			}
		}
		
		// deal with members of the object, and use an "infinite"
		// loop because we could have any amount of members
		while ( true )
		{
			if ( token.type == JsonToken.Type.STRING )
			{
				// the string value we read is the key for the object
				key = (String) token.value;
				
				// move past the string to see what's next
				nextValidToken();
				
				// after the string there should be a :
				if ( token.type == JsonToken.Type.COLON )
				{
					// move past the : and read/assign a value for the key
					nextToken();
					readValue(o, key);
					
					
					// move past the value to see what's next
					nextValidToken();
					
					// after the value there's either a } or a ,
					if ( token.type == JsonToken.Type.RIGHT_BRACE ){
						return (T)o;
					}
					else if ( token.type == JsonToken.Type.COMMA ){
						// skip past the comma and read another member
						nextToken();
					}
					else
					{
						tokenizer.parseError( "Expecting } or , but found " + token.value );
					}
				}
				else
				{
					tokenizer.parseError( "Expecting : but found " + token.value );
				}
			}
			else
			{
				tokenizer.parseError( "Expecting string but found " + token.value );
			}
		}
	}
	
	/**
	 * Attempt to parse a value
	 * @throws JsonParseError 
	 */
	private <T> T parseValue(Class<T> type) throws JsonParseError {
		
		checkValidToken();
		switch ( token.type ){
			case LEFT_BRACE:
				return parseObject(type);
			case LEFT_BRACKET:
				return parseArray(type);
			case STRING:
			case NUMBER:
			case TRUE:
			case FALSE:
			case NULL:
				return (T) token.readValue(type);
			default:
				tokenizer.parseError( "Unexpected " + token.value );
		}
		return null;
	}
}

