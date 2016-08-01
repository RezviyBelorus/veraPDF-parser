package org.verapdf.font.cff;

import org.verapdf.as.io.ASInputStream;
import org.verapdf.font.GeneralNumber;
import org.verapdf.font.cff.predefined.CFFStandardStrings;
import org.verapdf.io.InternalInputStream;

import java.io.IOException;

/**
 * This class does low-level parsing of CFF file.
 *
 * @author Sergey Shemyakov
 */
public class CFFFileBaseParser {

    private byte offSize;
    protected InternalInputStream source;
    protected CFFIndex definedNames;

    CFFFileBaseParser(ASInputStream source) throws IOException {
        this.source = new InternalInputStream(source);
    }

    CFFFileBaseParser(InternalInputStream source) {
        this.source = source;
    }

    protected byte readCard8() throws IOException {
        return this.source.readByte();
    }

    protected int readCard16() throws IOException {
        int highOrder = (this.source.readByte() & 0xFF) << 8;
        return highOrder | (this.source.readByte() & 0xFF);
    }

    protected long readOffset() throws IOException {
        return this.readOffset(this.offSize);
    }

    private long readOffset(int offSize) throws IOException {
        long res = 0;
        for (int i = 0; i < offSize - 1; ++i) {
            res |= (this.source.readByte() & 0xFF);
            res <<= 8;
        }
        res |= (this.source.readByte() & 0xFF);
        return res;
    }

    protected CFFIndex readIndex() throws IOException {
        int count = readCard16();
        if (count == 0) {
            return new CFFIndex(0, 0, new int[0], new byte[0]);
        }
        byte offSize = readCard8();
        int[] offset = new int[count + 1];
        for (int i = 0; i < count + 1; ++i) {
            offset[i] = (int) readOffset(offSize);
        }
        byte[] data = new byte[offset[count] - 1];
        if (source.read(data, data.length) != data.length) {
            throw new IOException("End of stream is reached");
        }
        int offsetShift = 3 + offSize * (count + 1);
        return new CFFIndex(count, offsetShift, offset, data);
    }

    protected void readHeader() throws IOException {
        readCard8();
        readCard8();
        byte hdrSize = readCard8();
        this.offSize = readCard8();
        this.source.seek(hdrSize);
    }

    private float readReal() throws IOException {
        StringBuilder builder = new StringBuilder();
        byte buf;
        parsing:
        while (true) {
            buf = readCard8();
            int[] hexs = new int[2];
            hexs[0] = buf >> 4;
            hexs[1] = buf & 0x0F;
            for (int i = 0; i < 2; ++i) {
                if (hexs[i] < 10) {
                    builder.append(hexs[i]);
                } else {
                    switch (hexs[i]) {
                        case 0x0A:
                            builder.append('.');
                            break;
                        case 0x0B:
                            builder.append('E');
                            break;
                        case 0x0C:
                            builder.append("E-");
                            break;
                        case 0x0E:
                            builder.append('-');
                            break;
                        case 0x0F:
                            break parsing;
                        default:    // Can not be reached
                            break parsing;
                    }
                }
            }
        }
        return Float.parseFloat(builder.toString());
    }

    private int readInteger(byte firstByte) throws IOException {
        int firstByteValue = firstByte & 0xFF;
        if (firstByteValue > 31 && firstByteValue < 247) {
            return firstByteValue - 139;
        }
        if (firstByteValue > 246 && firstByteValue < 251) {
            int first = (firstByteValue - 247) << 8;
            return first + readCard8() + 108;
        }
        if (firstByteValue > 250 && firstByteValue < 255) {
            int first = (firstByteValue - 251) << 8;
            return -first - readCard8() - 108;
        }
        if (firstByteValue == 28) {
            return readCard16();
        }
        if (firstByteValue == 29) {
            return (readCard16() << 16) | readCard16();
        } else {    // Shouldn't be reached
            throw new IOException("Can't read integer");
        }
    }

    protected GeneralNumber readNumber() throws IOException {
        byte first = this.source.readByte();
        if(first == 0x1E) {
            return new GeneralNumber(this.readReal());
        } else {
            return new GeneralNumber(this.readInteger(first));
        }
    }

    protected String getStringBySID(int sid) throws IOException {
        try {
            if (sid < CFFStandardStrings.N_STD_STRINGS) {
                return CFFStandardStrings.STANDARD_STRINGS[sid];
            } else {
                return new String(this.definedNames.get(sid -
                        CFFStandardStrings.N_STD_STRINGS));
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IOException("Can't get string with given SID", e);
        }
    }
}
