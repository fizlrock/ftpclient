package test_ui_project.Models.PathUtils;

import java.util.Arrays;

public class FileUnit {

	public boolean IsFile;
	public boolean IsFolder;
	public String Name;
	public int Size;
	public String FilePermissions;
	public String Extension = "None";
	public Icon icon = Icon.FOLDER;

	private static String[] image_ext = new String[] { "jpg", "png", "cvg" };
	private static String[] doc_ext = new String[] { "doc", "pdf", "txt" };
	private static String[] zip_ext = new String[] { "gz", "zip" };

	public FileUnit(String ls_line) {

		while (!ls_line.equals(ls_line.replace("  ", " "))) {
			ls_line = ls_line.replace("  ", " ");
		}
		String[] params = ls_line.split(" ");

		FilePermissions = params[0];
		Size = Integer.parseInt(params[4]);
		IsFolder = FilePermissions.toCharArray()[0] == 'd';
		IsFile = !IsFolder;
		Name = params[8];

		if (IsFile)
			setExtension();
	}

	private void setExtension() {

		if (!Name.contains("."))
			return;

		String[] temp = Name.split("\\.");
		System.out.println(Name);
		System.out.println(Arrays.toString(temp));
		if (temp.length > 0) {
			Extension = temp[temp.length - 1];
			if (Arrays.asList(image_ext).contains(Extension))
				icon = Icon.IMAGE;
			else if (Arrays.asList(doc_ext).contains(Extension))
				icon = Icon.DOCK;
			else
				icon = Icon.UNKNOWN;

		} else {
			icon = Icon.UNKNOWN;
		}
	}

	private FileUnit() {
		IsFile = false;
		IsFolder = true;
		Name = "";
		Size = 0;
		FilePermissions = "";
	}

	public static FileUnit getRoot() {
		return new FileUnit();
	}

	@Override
	public String toString() {
		if (IsFile) {
			return String.format(icon.Emoji + "%s (%s bytes) %s", Name, Size, Extension);
		} else {
			return String.format(" %s", Name);
		}
	}

	enum Icon {
		IMAGE("  "),
		DOCK("󰈙  "),
		FOLDER("  "),
		UNKNOWN("  ");

		public final String Emoji;

		Icon(String emoji) {
			Emoji = emoji;
		}

	}
}
