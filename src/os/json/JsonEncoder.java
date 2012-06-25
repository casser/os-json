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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import os.utils.StringUtils;
import os.utils.Types;


public class JsonEncoder
{
	Boolean formated;
	Boolean commented;
	
	public JsonEncoder(Boolean formated, Boolean commented) {
		this.formated = formated;
		this.commented = commented;
	}
	
	public JsonEncoder() {
		this(false,false);
	}

	public String encode( Object value)
	{
		return convertToString( value, 0 );
	}
	
	private String comment(String text){
		return commented?"/*"+text+"*/":"";
	}
	
	private String convertToString( Object value, int depth ) {
		// determine what value is and convert it based on it's type
		if(value ==null){
			return "null";	
		}else 
		if (String.class.isAssignableFrom(value.getClass())){
			return escapeString((String) value)+comment(value.getClass().getSimpleName());
		}else 
		if (Number.class.isAssignableFrom(value.getClass())){
			return((value!=null) ? value.toString() : "null")+comment(value.getClass().getSimpleName());
		}else 
		if (Class.class.isAssignableFrom(value.getClass())){
			return((value!=null) ? escapeString(((Class<?>)value).getName()) : "null")+comment(value.getClass().getSimpleName());
		}else 
		if (Enum.class.isAssignableFrom(value.getClass())){
			return((value!=null) ? escapeString(((Enum<?>)value).name()) : "null")+comment(value.getClass().getSimpleName());
		}else 
		if (Boolean.class.isAssignableFrom(value.getClass())){
			return ((Boolean)value ? "true" : "false")+comment(value.getClass().getSimpleName());
		}else 
		if (Date.class.isAssignableFrom(value.getClass())){
			// convert boolean to string easily
			return (new Long(((Date)value).getTime()).toString())+ comment(value.getClass().getSimpleName());
		}else 
		if (UUID.class.isAssignableFrom(value.getClass())){
			// convert boolean to string easily
			return escapeString(((UUID)value).toString())+comment(value.getClass().getSimpleName());
		}else 
		if (
			List.class.isAssignableFrom(value.getClass())||
			Set.class.isAssignableFrom(value.getClass())
		){
			// call the helper method to convert an array
			return arrayToString(value, depth )+comment(value.getClass().getSimpleName());
		}if (Annotation.class.isAssignableFrom(value.getClass())){
			return annotationToString((Annotation)value, depth )+comment(value.getClass().getSimpleName());
		}else{
			// call the helper method to convert an object
			return objectToString( value, depth  )+comment(value.getClass().getSimpleName());
		}
	}
	
	

	/**
	 * Escapes a string accoding to the JSON specification.
	 *
	 * @param str The string to be escaped
	 * @return The string with escaped special characters
	 * 		according to the JSON specification
	 */
	private  String escapeString( String str ) {		
		String s = "";
		char ch;
		int len = str.length();
		for ( int i = 0; i < len; i++ ) {
			ch = str.charAt( i );
			switch ( ch )
			{
				case '"': // quotation mark
					s += "\\\"";
					break;
				//case '/':	// solidus
				//	s += "\\/";
				//	break;
				case '\\': // reverse solidus
					s += "\\\\";
					break;
				case '\b': // bell
					s += "\\b";
					break;
				case '\f': // form feed
					s += "\\f";
					break;
				case '\n': // newline
					s += "\\n";
					break;
				case '\r': // carriage return
					s += "\\r";
					break;
				case '\t': // horizontal tab
					s += "\\t";
					break;
				default: // everything else
					// check for a control character and escape as unicode
					if ( ch < ' ' ){
						// get the hex digit(s) of the character (either 1 or 2 digits)
						String hexCode = Integer.toString((int)ch,16);
						
						// ensure that there are 4 digits by adjusting
						// the # of zeros accordingly.
						String zeroPad = hexCode.length() == 2 ? "00" : "000";
						
						// create the unicode escape sequence with 4 hex digits
						s += "\\u" + zeroPad + hexCode;
					}else{
						
						// no need to do any special encoding, just pass-through
						s += ch;
						
					}
			} // end switch
			
		} // end for loop
		
		return "\"" + s + "\"";
	}
	
	@SuppressWarnings("unchecked")
	private String arrayToString( Object a , int depth ) {
		String s = "";
		if (List.class.isAssignableFrom(a.getClass())){
			List<Object> list = ((List<Object>)a);
			if(list.size()>0){
				for ( int i = 0; i < list.size(); i++ ){
					s += convertToString(list.get(i), depth)+",";
				}
				s = s.substring(0,s.length()-1);
			}
		}else if(Set.class.isAssignableFrom(a.getClass())){
			Set<Object> list = ((Set<Object>)a);
			if(list.size()>0){
				for (Object value:list){
					s += convertToString(value, depth)+",";
				}
				s = s.substring(0,s.length()-1);
			}
		}
		return "[" + s + "]";
	}
	private String annotationToString(Annotation value, int depth) {
		String el = formated?"\n":"";
		String d0 = formated?StringUtils.repeat("  ", depth)  :"";
		String d1 = formated?StringUtils.repeat("  ", depth+1):"";
		String s = "";
		
		
		
		
		Method[] methods = value.annotationType().getDeclaredMethods();
		for(Method method:methods){
			try {
				s += d1+escapeString( method.getName() ) + ":" + convertToString( method.invoke(value, new Object[0]), depth+1)+","+el;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
		if(s.length()>1){
			s = s.substring(0,s.length()-(formated?2:1))+el;
			return "{"+el + s + d0+"}";
		}else{
			return "\"true\"";
		}
	}
	@SuppressWarnings({ "unchecked" })
	private String objectToString( Object o , int depth ) {
		String el = formated?"\n":"";
		String d0 = formated?StringUtils.repeat("  ", depth)  :"";
		String d1 = formated?StringUtils.repeat("  ", depth+1):"";
		String s = "";
		
		
		if(JsonEncodable.class.isAssignableFrom(o.getClass())){
			return ((JsonEncodable)o).encodeJson();
		}
		Types.Type type = Types.getType(o.getClass());		
		if(type.isSimple()){
			return "\""+o.toString()+"\"";
		}else if (type.isMap()){
			Map<Object,Object> map = (Map<Object,Object>)o;
			for(Map.Entry<Object, Object> entry:map.entrySet()){
				if(entry.getValue()!=null){
					s += d1+escapeString( entry.getKey().toString() ) + ":" + convertToString( entry.getValue(), depth+1)+","+el;
				}
			}
			
		}else if(type.isBean()){
			Map<String, Types.Property> properties = type.getProperties();
			for(Map.Entry<String, Types.Property> entry:properties.entrySet()){
				Types.Property property = entry.getValue();
				Object val = property.invokeGetter(o);
				if(val!=null){
					s += d1+escapeString( entry.getKey().toString() ) + ":" + convertToString( val,depth+1 )+","+el;
				}
			}
		}
		if(s.length()>1){
			s = s.substring(0,s.length()-(formated?2:1))+el;
			return "{"+el + s + d0+"}";
		}else{
			return "";
		}
	}
	
}
