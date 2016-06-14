package org.verapdf.cos;

import org.verapdf.as.ASAtom;
import org.verapdf.as.io.ASInputStream;
import org.verapdf.as.io.ASOutputStream;
import org.verapdf.cos.xref.COSFilterRegistry;
import org.verapdf.pd.PDObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Timur Kamalov
 */
public class COSFilters extends PDObject {

	private List<ASAtom> entries;

	public COSFilters() {
		super();
		this.entries = new ArrayList<>();
	}

	public COSFilters(final COSObject object) {
		this();
		setObject(object);
	}

	public ASInputStream getInputStream(ASInputStream inputStream) throws IOException {
		for (ASAtom asAtom : entries) {
			inputStream = COSFilterRegistry.getDecodeFilter(asAtom, inputStream);

			//TODO : if (!is.Get()) break;
		}
		return inputStream;
	}

	public ASOutputStream getOutputStream(ASOutputStream outputStream) throws IOException {
		for (ASAtom asAtom : entries) {
			outputStream = COSFilterRegistry.getEncodeFilter(asAtom, outputStream);

			//TODO : if (!is.Get()) break;
		}
		return outputStream;
	}

	public int size() {
		return this.entries.size();
	}

	public List<ASAtom> getFilters() {
		return entries;
	}

	protected void updateToObject() {
		COSObject filters = getObject();

		filters.clearArray();

		for (int i = 0; i < this.entries.size(); i++) {
			filters.add(COSName.construct(this.entries.get(i)));
		}
	}

	protected void updateFromObject() {
		COSObject filters = getObject();
		if(filters.getType().equals(COSObjType.COSArrayT)) {
			int size = filters.size();

			this.entries.clear();

			for (int i = 0; i < size; i++) {
				this.entries.add(filters.at(i).getName());
			}
		} else if (filters.getType().equals(COSObjType.COSNameT)) {
			this.entries.clear();
			this.entries.add(filters.getName());
		}
	}

}