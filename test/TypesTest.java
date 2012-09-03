import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import os.json.JSON;



public class TypesTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testJsonTest() throws Exception {
		System.out.println(JSON.decode(new File("data/test-user.json")));
		System.out.println(JSON.decode("{_id:MD5.087141500000000020a5bad6}"));
	}
}
