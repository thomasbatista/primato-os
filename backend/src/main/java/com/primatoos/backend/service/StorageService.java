package com.primatoos.backend.service;

public interface StorageService {

    String upload(String key, byte[] content, String contentType);

    void delete(String url);
}
