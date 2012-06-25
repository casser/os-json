import java.util.ArrayList;
import java.util.List;

import model.User;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import os.json.JSON;
import os.utils.ByteArray;
import os.utils.BytesUtil;
import os.utils.MD5;
import os.utils.Types;



public class TypesTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		Types.register(User.class);
		List<Class<?>> classes = new ArrayList<Class<?>>();
		classes.add(Types.Type.Info.class);
		
		JSON.print(Types.getAnnotatedTypes(classes));
		System.out.println(BytesUtil.toHex(MD5.bytes("Hello")));
		
	}

}
