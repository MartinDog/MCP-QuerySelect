package com.pagoda.aiqueryselect.utils;

public class VersionComparator {
    private VersionComparator(){}
    public static boolean isLatestVersion(String ver1, String ver2){
        String[] ver1List = ver1.split("\\.");
        String[] ver2List = ver2.split("\\.");
        int maxLen = Math.max(ver1List.length, ver2List.length);
        for(int i=0;i<maxLen;i++){
            int v1 = i < ver1List.length ? Integer.parseInt(ver1List[i]) : 0;
            int v2 = i < ver2List.length ? Integer.parseInt(ver2List[i]) : 0;
            if(v1>v2){//최신
                return true;
            }
            if(v1<v2){//구버전
                return false;
            }
        }
        return true;
    }
}
