package jef.common.log;

import java.io.IOException;
import java.io.PrintStream;

import jef.tools.StringUtils;


public class LogPrintStream extends PrintStream {
	private boolean isErr;
	private org.slf4j.Logger log;
	PrintStream old;
	
	public LogPrintStream(org.slf4j.Logger log,boolean isError,PrintStream old) {
		super(old);
		this.isErr=isError;
		this.log=log;
		this.old=old;
	}
	
	
	private boolean directOut(StackTraceElement[] stackTrace) {
		String clz=stackTrace[1].getClassName();
		if(clz.startsWith("org.")){
			return true;
		}
		return false;
	}

	
	@Override
	public void print(boolean flag) {
		if(directOut(new Throwable().getStackTrace())){
			old.print(flag);
			return;
		}
		if(isErr){
			log.error(String.valueOf(flag));
		}else{
			log.info(String.valueOf(flag));			
		}
	}


	@Override
	public void print(char c) {
		if(directOut(new Throwable().getStackTrace())){
			old.print(c);
			return;
		}
		if(isErr){
			log.error(String.valueOf(c));
		}else{
			log.info(String.valueOf(c));			
		}
	}

	@Override
	public void print(int i) {
		if(directOut(new Throwable().getStackTrace())){
			old.print(i);
			return;
		}
		if(isErr){
			log.error(String.valueOf(i));
		}else{
			log.info(String.valueOf(i));			
		}
	}

	@Override
	public void print(long l) {
		if(directOut(new Throwable().getStackTrace())){
			old.print(l);
			return;
		}
		if(isErr){
			log.error(String.valueOf(l));
		}else{
			log.info(String.valueOf(l));			
		}
	}

	@Override
	public void print(float f) {
		if(directOut(new Throwable().getStackTrace())){
			old.print(f);
			return;
		}
		if(isErr){
			log.error(String.valueOf(f));
		}else{
			log.info(String.valueOf(f));
		}
	}

	@Override
	public void print(double d) {
		if(directOut(new Throwable().getStackTrace())){
			old.print(d);
			return;
		}
		if(isErr){
			log.error(String.valueOf(d));
		}else{
			log.info(String.valueOf(d));
		}
	}

	@Override
	public void print(char[] ac) {
		if(directOut(new Throwable().getStackTrace())){
			old.print(ac);
			return;
		}
		if(isErr){
			log.error(new String(ac));
		}else{
			log.info(new String(ac));
		}
	}

	@Override
	public void print(String s) {
		if(s==null)return;
		if(directOut(new Throwable().getStackTrace())){
			try {
				old.write(s.getBytes());
			} catch (IOException e) {
			}
			return;
		}
		if(isErr){
			log.error(s);
		}else{
			log.info(s);			
		}
	}

	@Override
	public void print(Object obj) {
		if(directOut(new Throwable().getStackTrace())){
			old.print(obj);
			return;
		}
		if(isErr){
			log.error(String.valueOf(obj));
		}else{
			log.info(String.valueOf(obj));			
		}
	}

	@Override
	public void println(boolean flag) {
		if(directOut(new Throwable().getStackTrace())){
			old.println(flag);
			return;
		}
		if(isErr){
			log.error(String.valueOf(flag));
		}else{
			log.info(String.valueOf(flag));			
		}
	}

	@Override
	public void println(char c) {
		if(directOut(new Throwable().getStackTrace())){
			old.println(c);
			return;
		}
		if(isErr){
			log.error(String.valueOf(c));
		}else{
			log.info(String.valueOf(c));			
		}
	}

	@Override
	public void println(int i) {
		if(directOut(new Throwable().getStackTrace())){
			old.println(i);
			return;
		}
		if(isErr){
			log.error(String.valueOf(i));
		}else{
			log.info(String.valueOf(i));			
		}
	}

	@Override
	public void println(long l) {
		if(directOut(new Throwable().getStackTrace())){
			old.println(l);
			return;
		}
		if(isErr){
			log.error(String.valueOf(l));
		}else{
			log.info(String.valueOf(l));			
		}
	}

	@Override
	public void println(float f) {
		if(directOut(new Throwable().getStackTrace())){
			old.println(f);
			return;
		}
		if(isErr){
			log.error(String.valueOf(f));
		}else{
			log.info(String.valueOf(f));			
		}
	}

	@Override
	public void println(double d) {
		if(directOut(new Throwable().getStackTrace())){
			old.println(d);
			return;
		}
		if(isErr){
			log.error(String.valueOf(d));
		}else{
			log.info(String.valueOf(d));			
		}
	}

	@Override
	public void println(char[] ac) {
		if(ac==null)return;
		if(directOut(new Throwable().getStackTrace())){
			old.println(ac);
			return;
		}
		if(isErr){
			log.error(new String(ac));
		}else{
			log.info(new String(ac));			
		}
	}

	@Override
	public void println(String s) {
		if(s==null)return;
		if(directOut(new Throwable().getStackTrace())){
			try {
				old.write(s.getBytes());
			} catch (IOException e) {
			}
			old.write(StringUtils.CR);
			return;
		}
		if(isErr){
			log.error(s);
		}else{
			log.info(s);			
		}
	}

	@Override
	public void println(Object obj) {
		if(directOut(new Throwable().getStackTrace())){
			old.println(obj);
			return;
		}
		if(isErr){
			log.error(String.valueOf(obj));
		}else{
			log.info(String.valueOf(obj));			
		}
	}
}
