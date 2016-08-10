package org.verapdf.font.truetype;

import org.verapdf.io.InternalInputStream;

import java.io.IOException;

/**
 * This class does parsing of True Type "cmap" table and extracts all the data
 * needed.
 *
 * @author Sergey Shemyakov
 */
class TrueTypeCmapTable extends TrueTypeTable {

    private TrueTypeCmapSubtable[] cmapInfos;

    TrueTypeCmapTable(InternalInputStream source, long offset) {
        super(source, offset);
    }

    TrueTypeCmapSubtable[] getCmapInfos() {
        return cmapInfos;
    }

    @Override
    public void readTable() throws IOException {
        long startingOffset = this.source.getOffset();
        this.source.seek(this.offset);
        this.source.skip(2);    // version
        int numberOfTables = this.readUShort();
        this.cmapInfos = new TrueTypeCmapSubtable[numberOfTables];
        for (int i = 0; i < numberOfTables; ++i) {
            this.cmapInfos[i] =
                    new TrueTypeCmapSubtable(this.readUShort(), this.readUShort(),
                            this.readULong());
        }
        for (TrueTypeCmapSubtable cmap : cmapInfos) {
            this.source.seek(cmap.getOffset());
            int format = this.readUShort();
            switch (format) {
                case 0:
                    readByteEncodingTable(cmap);
                    break;
                case 2:
                    readHighByteMapping(cmap);
                    break;
                case 4:
                    readSegmentMapping(cmap);
                    break;
                case 6:
                    readTrimmedTableMapping(cmap);
                    break;
            }
        }
        this.source.seek(startingOffset);
    }

    private void readByteEncodingTable(TrueTypeCmapSubtable cmap) throws IOException {
        this.source.skip(4);    // length, version
        for (int i = 0; i < 256; ++i) {
            cmap.put(i, this.readByte());
        }
    }

    private void readHighByteMapping(TrueTypeCmapSubtable cmap) throws IOException {
        //TODO: do we need this asian cmap type? Reasons against:
        // 1) It is not mapping ch. code -> GID in usual sense, it needs byte stream for proper work.
        // 2) In case of big asian font our width extraction will fail on previous stage of creating mapping ch. code -> name.
    }

    private void readSegmentMapping(TrueTypeCmapSubtable cmap) throws IOException {
        this.source.skip(4);    // length, version
        int segCount = this.readUShort() / 2;
        this.source.skip(6);    // searchRange, entrySelector, rangeShift
        int[] endCode = new int[segCount];
        int[] startCode = new int[segCount];
        int[] idDelta = new int[segCount];
        int[] idRangeOffset = new int[segCount];
        long idRangeOffsetBegin = initSegmentMapping(endCode,
                startCode, idDelta, idRangeOffset, segCount);
        for (int i = 0; i < segCount; ++i) {
            if (idRangeOffset[i] == 0) {
                for (int j = startCode[i]; j <= endCode[j]; ++j) {
                    cmap.put(j, idDelta[i] + j);
                }
            } else {
                for (int j = 0; j < endCode[i] - startCode[i]; ++j) {    // In the next line according to spec we need to write idRangeOffset[i]/2. Why?
                    long glyphOffset = j + idRangeOffset[i] + idRangeOffsetBegin + 2 * i;   // adding offset of i-th element in idRangeOffset
                    this.source.seek(glyphOffset);
                    int glyphCode = this.readUShort();
                    if (glyphCode != 0) {
                        glyphCode = (glyphCode + idDelta[i]) % 65536;
                    }
                    cmap.put(j, glyphCode);
                }
            }
        }
    }

    private long initSegmentMapping(int[] endCode, int[] startCode, int[] idDelta,
                                    int[] idRangeOffset, int segCount) throws IOException {
        for (int i = 0; i < segCount; ++i) {
            endCode[i] = this.readUShort();
        }
        this.source.skip(2);    // reserved
        for (int i = 0; i < segCount; ++i) {
            startCode[i] = this.readUShort();
        }
        for (int i = 0; i < segCount; ++i) {
            idDelta[i] = this.readUShort();
        }
        long idRangeBegin = this.source.getOffset();
        for (int i = 0; i < segCount; ++i) {
            idRangeOffset[i] = this.readUShort();
        }
        return idRangeBegin;
    }

    private void readTrimmedTableMapping(TrueTypeCmapSubtable cmap) throws IOException {
        this.source.skip(4);    // length, version
        int firstCode = this.readUShort();
        int entryCount = this.readUShort();
        for (int i = 0; i < entryCount; ++i) {
            cmap.put(firstCode + i, readUShort());
        }
    }
}
