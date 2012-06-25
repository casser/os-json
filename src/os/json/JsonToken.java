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

import os.utils.Types;

	
public final class JsonToken {
	
	public static enum Type {
		UNKNOWN,
		COMMA,
		LEFT_BRACE,
		RIGHT_BRACE,
		LEFT_BRACKET,
		RIGHT_BRACKET,
		COLON,
		TRUE,
		FALSE,
		NULL,
		STRING,
		NUMBER,
		NAN;
	}

	public Type type;
	public Object value;
	
	public JsonToken( Type type, Object value){
		this.type = type;
		this.value = value;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T readValue(Class<T> t){
		Object ret = null;
		
		if(t==null){
			switch(type){
				case NUMBER: t = (Class<T>) Double.class; break;
				case STRING: t = (Class<T>) String.class; break;
			}
		}
		
		if(t==null){
			return null;
		}
		
		if(Double.class.isAssignableFrom(t)){
			ret =  Double.valueOf(this.value.toString());
		}else
		if(Integer.class.isAssignableFrom(t)){
			ret =  Integer.valueOf(this.value.toString());
		}else
		if(Long.class.isAssignableFrom(t)){
			ret =  Long.valueOf(this.value.toString());
		}else
		if(t.equals(value.getClass())){
			ret = this.value;
		}else{
			ret =  convertObject(this.value,t);
		}
		return (T)ret; 
	}
	
	private Object convertObject(Object o, Class<?> cls) {
		Types.Type type = Types.getType(cls);
		if(type.isEnum()){
			return toEnum(o, type);
		}else
		if(type.isBean()){
			return toBean(o, type);
		}
		return type;
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
	
	static JsonToken create( Type type, Object value ){
		return new JsonToken(type, value);
	}
}
