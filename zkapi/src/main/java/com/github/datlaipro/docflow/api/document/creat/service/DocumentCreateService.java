package com.github.datlaipro.docflow.api.document.creat.service;

import com.github.datlaipro.docflow.api.document.creat.dto.DocumentReqIN;
import com.github.datlaipro.docflow.api.document.creat.dto.DocumentReqOUT;

public interface DocumentCreateService {
    long createOutbound(DocumentReqOUT req) throws Exception;
    long createInbound(DocumentReqIN req) throws Exception;
}
