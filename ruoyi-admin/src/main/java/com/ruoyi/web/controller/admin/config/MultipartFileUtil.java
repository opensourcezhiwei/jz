package com.ruoyi.web.controller.admin.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.springframework.web.multipart.MultipartFile;

public class MultipartFileUtil implements MultipartFile {
	private final byte[] imgContent;
	private final String header;

	public MultipartFileUtil(byte[] imgContent, String header) {
		this.imgContent = imgContent;
		this.header = header.split(";")[0];
	}

	@Override
	public String getName() {
		return System.currentTimeMillis() + Math.random() + "." + header.split("/")[1];
	}

	@Override
	public String getOriginalFilename() {
		return System.currentTimeMillis() + (int) Math.random() * 10000 + "." + header.split("/")[1];
	}

	@Override
	public String getContentType() {
		return header.split(":")[1];
	}

	@Override
	public boolean isEmpty() {
		return imgContent == null || imgContent.length == 0;
	}

	@Override
	public long getSize() {
		return imgContent.length;
	}

	@Override
	public byte[] getBytes() throws IOException {
		return imgContent;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(imgContent);
	}

	@Override
	public void transferTo(File dest) throws IOException, IllegalStateException {
		new FileOutputStream(dest).write(imgContent);
	}

	public static MultipartFile base64ToMultipart(String base64) {
		String[] baseStrs = base64.split(",");

		byte[] b = Base64.getDecoder().decode(baseStrs[1]);

		for (int i = 0; i < b.length; ++i) {
			if (b[i] < 0) {
				b[i] += 256;
			}
		}
		return new MultipartFileUtil(b, baseStrs[0]);
	}
}