package org.PhantomCamera.AprilTags;

import java.util.List;

public class AprilTag36h11Dictionary {

    public static class ApriltagCode {

        public final int tagId;
        public final long[] rotatedCodeBitArray;

        public ApriltagCode(int tagId, long[] rotatedCodeBitArray) {
            this.tagId = tagId;
            this.rotatedCodeBitArray = rotatedCodeBitArray;
        }

    }

    private final List<ApriltagCode> codeList;

    public AprilTag36h11Dictionary( List<ApriltagCode> codeList ) {
        this.codeList = codeList;
    }

    public List<ApriltagCode> getCodeList() {
        return codeList;
    }

}
