package os.json;

public class JsonParseError extends Exception {
	
	private static final long serialVersionUID = -3872954761146946303L;
	
	private int location;
	private String text;
	
	public JsonParseError( String message, int location, String text) {
		super( message );
		this.location = location;
		this.text = text;
	}
	
	public int getLocation(){
		return location;
	}
	
	public String getText(){
		return text;
	}
	
}

