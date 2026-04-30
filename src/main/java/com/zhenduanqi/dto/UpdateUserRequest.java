package com.zhenduanqi.dto;

import java.util.List;

public class UpdateUserRequest {

    private String realName;
    private String status;
    private List<String> roleCodes;

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getRoleCodes() { return roleCodes; }
    public void setRoleCodes(List<String> roleCodes) { this.roleCodes = roleCodes; }
}
