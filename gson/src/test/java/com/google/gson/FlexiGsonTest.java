package com.google.gson;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;


public class FlexiGsonTest {
	
	@Test
	public void testConvert() {
		Gson gson = new GsonBuilder().create();
		DummyObject dummy1 = new DummyObject();
		dummy1.a = "Hello";
		dummy1.b = 7;
		dummy1.cs.add("C");
		String json = gson.toJson(dummy1);
		DummyObject dummy2 = gson.fromJson(json, DummyObject.class);
		assert dummy2.equals(dummy1);
		
		Map map = new HashMap();
		map.put("a", "Hello");
		map.put("b", 7);
		map.put("cs", new String[]{"C"});
		
		DummyObject dummy3 = gson.convert(map, DummyObject.class);
		System.out.println(dummy3);
		assert dummy1.equals(dummy3);		
	}

	@Test
	public void testPreserveClass() {
		Gson gson = new GsonBuilder()
//						.setClassProperty("@class")
						.create();
		DummyObject dummy1 = new DummyObject();
		dummy1.a = "Hello";
		dummy1.b = 7;
		dummy1.cs.add("C");
		String json = gson.toJson(dummy1);
		
		assert json.contains("DummyObject") : json;
		System.out.println(json);
		
		DummyObject dummy2 = gson.fromJson(json);
		assert dummy2.equals(dummy1);		
	}
}


class DummyObject {
	@Override
	public String toString() {
		return "DummyObject [a=" + a + ", b=" + b + ", cs=" + cs + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + b;
		result = prime * result + ((cs == null) ? 0 : cs.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DummyObject other = (DummyObject) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		if (b != other.b)
			return false;
		if (cs == null) {
			if (other.cs != null)
				return false;
		} else if (!cs.equals(other.cs))
			return false;
		return true;
	}
	String a;
	int b;
	List<String> cs = new ArrayList();
}