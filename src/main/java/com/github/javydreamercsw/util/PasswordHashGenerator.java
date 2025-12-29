/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Utility to generate BCrypt password hashes. Run this to generate hashes for default accounts. */
public class PasswordHashGenerator {

  public static void main(String[] args) {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    System.out.println("Generating BCrypt hashes with 10 rounds:");
    System.out.println();

    String[] passwords = {"admin123", "booker123", "player123", "viewer123"};
    String[] usernames = {"admin", "booker", "player", "viewer"};

    for (int i = 0; i < passwords.length; i++) {
      String hash = encoder.encode(passwords[i]);
      System.out.println(usernames[i] + " password: " + passwords[i]);
      System.out.println("BCrypt hash: " + hash);
      System.out.println();
    }
  }
}
