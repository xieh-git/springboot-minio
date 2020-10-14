package com.xieh.springbootminio.controller;

import com.alibaba.fastjson.JSON;

import com.xieh.springbootminio.prop.MinioProperties;
import com.xieh.springbootminio.vo.Res;
import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.PutObjectOptions;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/minio")
public class MinioController {
    @Autowired
    private MinioClient minioClient;
    
    @Autowired
    private MinioProperties minioProperties;


    /**
     * 获取文件列表
     * @param map
     * @return
     * @throws Exception
     */
    @GetMapping("/list")
    public List<Object> list(ModelMap map) throws Exception {
        Iterable<Result<Item>> myObjects = minioClient.listObjects(minioProperties.getBucketName());
        Iterator<Result<Item>> iterator = myObjects.iterator();
        List<Object> items = new ArrayList<>();
        String format = "{'fileName':'%s','fileSize':'%s'}";
        while (iterator.hasNext()) {
            Item item = iterator.next().get();
            items.add(JSON.parse(String.format(format, item.objectName(), formatFileSize(item.size()))));
        }
        return items;
    }

    /**
     * 上传文件
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public Res upload(@RequestParam(name = "file", required = false) MultipartFile[] file) throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException, RegionConflictException {
        Res res = new Res();
        res.setCode(500);

        if (file == null || file.length == 0) {
            res.setMessage("上传文件不能为空");
            return res;
        }

        //存入bucket不存在则创建，并设置为只读
        if (!minioClient.bucketExists(minioProperties.getBucketName())) {
            minioClient.makeBucket(minioProperties.getBucketName());
            //minioClient.setBucketPolicy(minioProperties.getBucketName(), "*.*", PolicyType.READ_ONLY);
        }

        List<String> orgfileNameList = new ArrayList<>(file.length);

        for (MultipartFile multipartFile : file) {
            // 获取真实文件名
            String orgfileName = multipartFile.getOriginalFilename();
            // 重命名文件
            String fileNewName= UUID.randomUUID().toString() +orgfileName.substring(orgfileName.lastIndexOf('.'));
            // 父目录名
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            // 文件存储的目录结构
            String objectName = sdf.format(new Date()) + "/" + fileNewName;

            try {
                InputStream in = multipartFile.getInputStream();
                // 开始上传
                minioClient.putObject(minioProperties.getBucketName(), objectName, in, new PutObjectOptions(in.available(), -1));
                in.close();
                // 完成上传以后再保存文件完整路径
                String fullFilePath = minioProperties.getEndpoint() + "/" + minioProperties.getBucketName() + "/" + objectName;
                orgfileNameList.add(fullFilePath);
            } catch (Exception e) {
                log.error(e.getMessage());
                res.setMessage("上传失败");
                return res;
            }
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("bucketName", minioProperties.getBucketName());
        data.put("fileName", orgfileNameList);
        res.setCode(200);
        res.setMessage("上传成功");
        res.setData(data);
        return res;
    }

    /**
     * 下载文件
     * @param response
     * @param fileName
     */
    @RequestMapping("/download")
    public void download(HttpServletResponse response, @RequestParam("fileName") String fileName) {
        InputStream in = null;
        try {
            String name = "";
            System.out.println("download-fileName:" + fileName);
            //文件名截取
            if(fileName.lastIndexOf("/") != -1 ){
                name = fileName.substring(fileName.lastIndexOf("/") + 1);
            }else{
                name = fileName;
            }
            ObjectStat stat = minioClient.statObject(minioProperties.getBucketName(), fileName);
            response.setContentType(stat.contentType());
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(name, "UTF-8"));

            in = minioClient.getObject(minioProperties.getBucketName(), fileName);
            IOUtils.copy(in, response.getOutputStream());
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    /**
     * 删除文件
     * @param fileName
     * @return
     */
    @DeleteMapping("/delete")
    public Res delete(@RequestParam("fileName") String fileName) {
        Res res = new Res();
        res.setCode(200);
        try {
            minioClient.removeObject(minioProperties.getBucketName(), fileName);
        } catch (Exception e) {
            res.setCode(500);
            log.error(e.getMessage());
        }
        return res;
    }


    /**
     * 生成可以预览的文件链接
     * @return
     * @throws XmlParserException
     * @throws NoSuchAlgorithmException
     * @throws InsufficientDataException
     * @throws InternalException
     * @throws InvalidResponseException
     * @throws InvalidKeyException
     * @throws InvalidBucketNameException
     * @throws ErrorResponseException
     * @throws IOException
     * @throws InvalidExpiresRangeException
     */
    @GetMapping("/previewList")
    public List<Object> getPreviewList() throws XmlParserException, NoSuchAlgorithmException, InsufficientDataException, InternalException, InvalidResponseException, InvalidKeyException, InvalidBucketNameException, ErrorResponseException, IOException, InvalidExpiresRangeException {
        Iterable<Result<Item>> myObjects = minioClient.listObjects(minioProperties.getBucketName());
        Iterator<Result<Item>> iterator = myObjects.iterator();
        List<Object> items = new ArrayList<>();
        String format = "{'fileName':'%s'}";
        while (iterator.hasNext()) {
            Item item = iterator.next().get();
            System.out.println("item.objectName():" + item.objectName());
            // TODO 根据文件后缀名，过滤哪些是可以预览的文件
            //String bucketName, 桶名称
            // String objectName, 文件路径
            // Integer expires, 链接过期时间
            // Map<String, String> reqParams 请求参数
            // 开始生成
            String filePath = minioClient.presignedGetObject(minioProperties.getBucketName(), item.objectName());
            items.add(JSON.parse(String.format(format, filePath)));
        }
        return  items;
    }



    /**
     * 显示文件大小信息单位
     * @param fileS
     * @return
     */
    private static String formatFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + " B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + " KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + " MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + " GB";
        }
        return fileSizeString;
    }
}