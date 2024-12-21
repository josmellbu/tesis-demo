package com.example.billing.common;

import io.swagger.v3.oas.annotations.media.Schema;

public class StandarizedApiExceptionResponse{
    @Schema(description = "The unique uri identifier that categorizes the error", name = "type", 
           required = true, example = "/errors/authentication/not-authorized")
    private String type ="/errors/uncategorized";
    @Schema(description = "A brief, human-readable message about the error", name = "title", 
           required = true, example = "The user does not have autorization")
    private String title;
    @Schema(description = "The unique error code", name = "code", 
           required = false, example = "192")
    private String code;
    @Schema(description = "A human-readable explanation of the error", name = "detail", 
           required = true, example = "The user does not have the propertly persmissions to acces the "
                   + "resource, please contact with ass https://digitalthinking.biz/es/ada-enterprise-core#contactus")
    private String detail;
    @Schema(description = "A URI that identifies the specific occurrence of the error", name = "detail", 
           required = true, example = "/errors/authentication/not-authorized/01")
    private String instance ="/errors/uncategorized/bank";

    public StandarizedApiExceptionResponse(String title, String code, String detail) {
        super();
        this.title = title;
        this.code = code;
        this.detail = detail;
    }       
       
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return code;
    }

    public void setStatus(String status) {
        this.code = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }
}
