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
package com.github.javydreamercsw.management.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QrCodeUtil {

  public static String toBase64Png(final String content, final int size)
      throws WriterException, IOException {
    QRCodeWriter writer = new QRCodeWriter();
    BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
    BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    // Use a MemoryCacheImageOutputStream instead of ImageIO.write(OutputStream): the latter wraps
    // the stream in a FileCacheImageOutputStream that creates a transient imageio*.tmp file in
    // java.io.tmpdir. On the production Tomcat install that directory lives under the launchd
    // WatchPaths of com.atwrpg.relink-tomcat, so every QR generation would restart the whole
    // server (ATW-sevh).
    MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out);
    ImageIO.write(image, "png", ios);
    ios.flush();
    return Base64.getEncoder().encodeToString(out.toByteArray());
  }
}
