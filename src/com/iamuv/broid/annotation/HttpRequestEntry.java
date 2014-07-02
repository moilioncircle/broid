/*
 * Copyright (C) 2014 The Broid Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iamuv.broid.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.iamuv.broid.http.HttpRequest;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HttpRequestEntry {

    public String mode() default "GET";

    public boolean cache() default true;

    public long session() default 60 * 1000;

    public int connectionTimeout() default HttpRequest.MAX_CONNECTION_TIMEOUT - 20 * 1000;

    public int socketTimeout() default HttpRequest.MAX_SOCKET_TIMEOUT - 20 * 1000;

    public String url() default "url";

    public String charset() default "UTF-8";

}
