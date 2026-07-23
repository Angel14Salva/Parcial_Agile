package com.municipalidad.licencias.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}")    String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key",    apiKey,
            "api_secret", apiSecret
        ));
    }

    @SuppressWarnings("unchecked")
    public String subirArchivo(MultipartFile archivo, String folder) throws IOException {
        // Los PDF subidos como resource_type "auto"/"image" caen bajo la
        // restriccion de seguridad de Cloudinary que bloquea con 401 la entrega
        // publica de PDF/ZIP como imagen. Subiendolos como "raw" se entregan
        // directo (URL /raw/upload/...), sin esa restriccion. Las imagenes (jpg/png)
        // siguen como "image" para conservar la vista previa.
        String nombre = archivo.getOriginalFilename();
        boolean esPdf = nombre != null && nombre.toLowerCase().endsWith(".pdf");
        Map<String, Object> options = ObjectUtils.asMap(
            "folder",        "licencias/" + folder,
            "resource_type", esPdf ? "raw" : "image"
        );
        Map<String, Object> result = cloudinary.uploader()
            .upload(archivo.getBytes(), options);
        return (String) result.get("secure_url");
    }

    public void eliminarArchivo(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            // ignorar errores al eliminar
        }
    }
}
