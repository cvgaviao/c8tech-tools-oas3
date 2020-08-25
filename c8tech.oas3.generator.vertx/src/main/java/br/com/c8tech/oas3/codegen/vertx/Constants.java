/*
 * ============================================================================
 *  Copyright ©  2020,    Cristiano V. Gavião
 *
 *  All rights reserved.
 *  This program and the accompanying materials are made available under
 *  the terms of the Eclipse Public License v1.0 which accompanies this
 *  distribution and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * ============================================================================
 */
package br.com.c8tech.oas3.codegen.vertx;

public class Constants {

  static final String DEFAULT_PACKAGE_API     = Constants.DEFAULT_PACKAGE_BASE + ".api";
  static final String DEFAULT_PACKAGE_BASE    = "br.com.c8tech.project";
  static final String DEFAULT_PACKAGE_INVOKER = DEFAULT_PACKAGE_BASE + ".exec";
  static final String DEFAULT_PACKAGE_MODEL   = DEFAULT_PACKAGE_BASE + ".model";
  static final String JAVA_EXTENSION          = ".java";
  static final String TEMPLATE_FOLDER         = "vertx-oas3";

  private Constants() {
  }

}
