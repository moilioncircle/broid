package com.iamuv.broid.storage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class StorageEntry {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface SQLiteTable {
		// 所在数据库名 默认为"database.sqlite"
		public String database() default "database.sqlite";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface SQLiteField {

		// 数据库字段默认值
		public String value() default "";

		// 是否为自增主键
		public boolean isAutoKey() default false;

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Preferences {
		// 配置文件打开模式
		public String mode() default "PRIVATE";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface PreferenceField {
		// 默认值
		public String value() default "";

	}

}
