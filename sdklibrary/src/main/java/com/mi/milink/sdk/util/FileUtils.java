package com.mi.milink.sdk.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 文件工具集
 *
 * @author MK
 */
public class FileUtils {
	public static final int ZIP_BUFFER_SIZE = 4 * 1024;

	public static final int CPY_BUFFER_SIZE = 4 * 1024;

	public static final String ZIP_FILE_EXT = ".zip";

	/**
	 * 复制文件
	 *
	 * @param srcFile
	 *            源文件路径
	 * @param dstFile
	 *            目标文件路径
	 * @return
	 */
	public static boolean copyFile(File srcFile, File dstFile) {
		boolean resu = false;

		FileInputStream fis = null;
		BufferedOutputStream fos = null;

		try {
			fis = new FileInputStream(srcFile);
			fos = new BufferedOutputStream(new FileOutputStream(dstFile));

			byte[] buffer = new byte[CPY_BUFFER_SIZE];

			int readLen = 0;

			while (-1 != (readLen = fis.read(buffer))) {
				fos.write(buffer, 0, readLen);
			}

			fos.flush();

			resu = true;
		} catch (IOException e) {
			resu = false;
		} finally {
			CommonUtils.closeDataObject(fos);
			CommonUtils.closeDataObject(fis);
		}

		return resu;
	}

	/**
	 * 尝试删除文件/文件夹。如果删除失败，尝试在虚拟机退出时删除。
	 *
	 * @param fileName
	 *            文件/文件夹路径
	 * @return 删除成功/失败
	 */
	public static boolean deleteFile(File file) {
		if (file != null) {
			// 是文件，直接删除
			if (file.isFile()) {
				if (!file.delete()) {
					file.deleteOnExit();

					return false;
				} else {
					return true;
				}
			}
			// 是目录，递归删除
			else if (file.isDirectory()) {
				File[] subFiles = file.listFiles();
				if (subFiles != null) {
					for (File subFile : subFiles) {
						deleteFile(subFile);
					}
				}
				return file.delete();
			}
			// 那你是啥嘛……
			else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * ZIP压缩多个文件/文件夹
	 *
	 * @param srcFiles
	 *            要压缩的文件/文件夹列表
	 * @param dest
	 *            目标文件
	 * @return 压缩成功/失败
	 */
	public static boolean zip(File[] srcFiles, File dest) {
		// 参数检查
		if (srcFiles == null || srcFiles.length < 1 || dest == null) {
			return false;
		}

		boolean resu = false;

		ZipOutputStream zos = null;

		try {
			byte[] buffer = new byte[ZIP_BUFFER_SIZE];

			zos = new ZipOutputStream(new BufferedOutputStream(
					new FileOutputStream(dest, false)));

			// 添加文件到ZIP压缩流
			for (File src : srcFiles) {
				doZip(zos, src, null, buffer);
			}

			zos.flush();
			zos.closeEntry();

			resu = true;
		} catch (IOException e) {
			// e.print*StackTrace();

			resu = false;
		} finally {
			CommonUtils.closeDataObject(zos);
		}

		return resu;
	}

	/**
	 * 方法：ZIP压缩单个文件/文件夹
	 *
	 * @param source
	 *            源文件/文件夹
	 * @param dest
	 *            目标文件
	 * @return 压缩成功/失败
	 */
	public static boolean zip(File src, File dest) {
		return zip(new File[] { src }, dest);
	}

	/**
	 * 方法：解压缩单个ZIP文件
	 *
	 * @param source
	 *            源文件/文件夹
	 * @param dest
	 *            目标文件夹
	 * @return 解压缩成功/失败
	 */
	public static boolean unzip(File src, File destFolder) {
		if (src == null || src.length() < 1 || !src.canRead()) {
			return false;
		}

		boolean resu = false;

		if (!destFolder.exists()) {
			destFolder.mkdirs();
		}

		ZipInputStream zis = null;

		BufferedOutputStream bos = null;

		ZipEntry entry = null;

		byte[] buffer = new byte[8 * 1024];

		int readLen = 0;

		try {
			zis = new ZipInputStream(new FileInputStream(src));

			while (null != (entry = zis.getNextEntry())) {
				System.out.println(entry.getName());

				if (entry.isDirectory()) {
					new File(destFolder, entry.getName()).mkdirs();
				} else {
					File entryFile = new File(destFolder, entry.getName());

					entryFile.getParentFile().mkdirs();

					bos = new BufferedOutputStream(new FileOutputStream(
							entryFile));

					while (-1 != (readLen = zis.read(buffer, 0, buffer.length))) {
						bos.write(buffer, 0, readLen);
					}

					bos.flush();
					bos.close();
				}
			}

			zis.closeEntry();
			zis.close();

			resu = true;
		} catch (IOException e) {
			resu = false;
		} finally {
			CommonUtils.closeDataObject(bos);
			CommonUtils.closeDataObject(zis);
		}

		return resu;
	}

	/**
	 * 压缩文件/文件夹到ZIP流中 <br>
	 * <br>
	 * <i>本方法是为了向自定义的压缩流添加文件/文件夹，若只是要压缩文件/文件夹到指定位置，请使用 {@code FileUtils.zip()}
	 * 方法</i>
	 *
	 * @param zos
	 *            ZIP输出流
	 * @param file
	 *            被压缩的文件
	 * @param root
	 *            被压缩的文件在ZIP文件中的入口根节点
	 * @param buffer
	 *            读写缓冲区
	 * @throws IOException
	 *             读写流时可能抛出的I/O异常
	 */
	public static void doZip(ZipOutputStream zos, File file, String root,
			byte[] buffer) throws IOException {
		// 参数检查
		if (zos == null || file == null) {
			throw new IOException("I/O Object got NullPointerException");
		}

		if (!file.exists()) {
			throw new FileNotFoundException("Target File is missing");
		}

		BufferedInputStream bis = null;

		int readLen = 0;

		String rootName = CommonUtils.isTextEmpty(root) ? (file.getName())
				: (root + File.separator + file.getName());

		// 文件直接放入压缩流中
		if (file.isFile()) {
			try {
				bis = new BufferedInputStream(new FileInputStream(file));

				zos.putNextEntry(new ZipEntry(rootName));

				while (-1 != (readLen = bis.read(buffer, 0, buffer.length))) {
					zos.write(buffer, 0, readLen);
				}

				CommonUtils.closeDataObject(bis);
			} catch (IOException e) {
				CommonUtils.closeDataObject(bis);
				// 关闭BIS流，并抛出异常
				throw e;
			}
		}
		// 文件夹则子文件递归
		else if (file.isDirectory()) {
			File[] subFiles = file.listFiles();

			for (File subFile : subFiles) {
				doZip(zos, subFile, rootName, buffer);
			}
		}
	}

	public static boolean unjar(File src, File destFolder) {
		if (src == null || src.length() < 1 || !src.canRead()) {
			return false;
		}

		boolean resu = false;

		if (!destFolder.exists()) {
			destFolder.mkdirs();
		}

		JarInputStream zis = null;

		BufferedOutputStream bos = null;

		JarEntry entry = null;

		byte[] buffer = new byte[8 * 1024];

		int readLen = 0;

		try {
			zis = new JarInputStream(new FileInputStream(src));

			while (null != (entry = zis.getNextJarEntry())) {
				System.out.println(entry.getName());

				if (entry.isDirectory()) {
					new File(destFolder, entry.getName()).mkdirs();
				} else {
					bos = new BufferedOutputStream(new FileOutputStream(
							new File(destFolder, entry.getName())));

					while (-1 != (readLen = zis.read(buffer, 0, buffer.length))) {
						bos.write(buffer, 0, readLen);
					}

					bos.flush();
					bos.close();
				}
			}

			zis.closeEntry();
			zis.close();

			resu = true;
		} catch (IOException e) {
			resu = false;
		} finally {
			CommonUtils.closeDataObject(bos);
			CommonUtils.closeDataObject(zis);
		}

		return resu;
	}

	public static void saveBytes2File(File f, byte[] b) throws Exception {

		if (f == null) {
			throw new NullPointerException("file is null ");
		}
		if (!f.exists()) {
			f.createNewFile();
		}

		if (!f.canWrite()) {
			throw new IOException("file " + f.getAbsolutePath()
					+ " is not writeable");
		}

		FileOutputStream fos = new FileOutputStream(f);
		try {
			fos.write(b);
		} finally {
			CommonUtils.closeDataObject(fos);
		}
	}

	public static byte[] readBytesFromFile(File f) throws Exception {

		if (f == null) {
			throw new NullPointerException("file is null ");
		}

		if (!f.exists()) {
			throw new FileNotFoundException("file " + f.getAbsolutePath()
					+ " is not exist");
		}

		if (!f.canRead()) {
			throw new IOException("file " + f.getAbsolutePath()
					+ " is not readable");
		}

		FileInputStream fis = new FileInputStream(f);
		byte[] b = new byte[512];// 本来文件就不大
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			int len = 0;
			while ((len = fis.read(b)) > 0) {
				bos.write(b, 0, len);

			}

			byte[] arr = bos.toByteArray();

			return arr;

		} finally {
			CommonUtils.closeDataObject(fis);
			CommonUtils.closeDataObject(bos);
			b = null;
		}

	}

	public static void writeToFile(File dst, String content) throws IOException {
		if (dst == null || content == null) {
			return;
		}
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(dst));
			bw.write(content);
		} finally {
			CommonUtils.closeDataObject(bw);
		}
	}

	public static String readStringFromFile(File source) throws IOException {
		if (source == null) {
			return null;
		}

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(source));
			StringBuilder sb = new StringBuilder();
			String str = null;
			while ((str = br.readLine()) != null) {
				sb.append(str);
				sb.append("\n");
			}
			return sb.toString();
		} finally {
			CommonUtils.closeDataObject(br);
		}
	}

}
