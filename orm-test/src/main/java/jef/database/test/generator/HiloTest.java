package jef.database.test.generator;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import jef.database.Sequence;
import jef.database.SequenceHiloGenerator;

import org.junit.Test;

public class HiloTest extends org.junit.Assert{
	private static Sequence s = new Sequence() {
		private AtomicInteger i = new AtomicInteger();

		public void pushBack(long key) {
			throw new UnsupportedOperationException();
		}

		public long next() {
			return i.getAndIncrement();
		}

		public void clear() {
			i.set(0);
		}

		public boolean isTable() {
			return false;
		}

		public String getName() {
			return "DEFAULT";
		}

		public boolean checkSequenceValue(String table, String columnName) throws SQLException {
			return true;
		}

		public boolean isRawNative() {
			return false;
		}
	};

	@Test
	public void testHilo() {
		SequenceHiloGenerator hilo = new SequenceHiloGenerator(s, 3);
		long max=0;
		for(int i=0;i<200;i++){
			max=hilo.next();
		}
		assertEquals(199, max);
	}

}
