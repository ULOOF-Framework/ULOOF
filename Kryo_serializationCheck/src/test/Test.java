package test;

import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

public class Test {

	public static void main(String[] args) {
		ArrayList<String> unserializable = new ArrayList<String>();
		unserializable.add("asdasda");
		KryoFactory factory = new KryoFactory() {
			@Override
			public Kryo create() {
				return new Kryo(unserializable);
			}
		};
		KryoPool pool = new KryoPool.Builder(factory).build();
		Kryo kryoInst = pool.borrow();
		kryoInst.setReferences(false);
		System.out.println("kryoInst.getDepth();= "+kryoInst.getReferences());
		Kryo kryoInst2 = pool.borrow();
		System.out.println("kryoInst.getDepth();= "+kryoInst2.getReferences());
		pool.release(kryoInst);
		pool.release(kryoInst2);
		System.out.println("kryoInst.getDepth();= asdasd "+pool.borrow().getReferences());
		

	}

}
