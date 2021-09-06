package com.inyabass.catspaw.api;

import com.inyabass.catspaw.logging.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.lang.invoke.MethodHandles;

@SpringBootApplication
public class Application {

	private final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(Application.class, args);
	}
}
