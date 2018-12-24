package jef.codegen;
final class EnhancedException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
	}