package jef.database.datasource;

public class DummyPasswordDecryptor implements PasswordDecryptor{
	@Override
	public String decrypt(String raw) {
		return raw;
	}
}
