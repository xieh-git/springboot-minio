package com.xieh.springbootminio.controller;

import com.alibaba.fastjson.JSON;
import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author 谢辉
 * @Classname MinioDemoController
 * @Description TODO
 * @Date 2020/10/13 22:03
 */
@RestController
@RequestMapping("/minioDemo")
public class MinioDemoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioDemoController.class);

    @Value("${minio.endpoint}")
    private  String ENDPOINT;
    @Value("${minio.bucketName}")
    private  String BUCKETNAME;
    @Value("${minio.accessKey}")
    private  String ACCESSKEY;
    @Value("${minio.secretKey}")
    private  String SECRETKEY;

    /**
     * 上传文件
     * @param file 文件
     * @return 返回文件完整地址
     */
    @PostMapping("/demoupload")
    public String upload(MultipartFile file) {
        String s=null;
        try {
            MinioClient minioClient = new MinioClient(ENDPOINT, ACCESSKEY, SECRETKEY);
            //存入bucket不存在则创建，并设置为只读
            if (!minioClient.bucketExists(BUCKETNAME)) {
                minioClient.makeBucket(BUCKETNAME);
                //minioClient.setBucketPolicy(BUCKETNAME, "*.*", PolicyType.READ_ONLY);
            }
            // 获取文件名
            String filename = file.getOriginalFilename();
            // 重命名文件
            String fileNewName= UUID.randomUUID().toString() +filename.substring(filename.lastIndexOf('.'));
            // 父目录
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            // 文件存储的目录结构
            String objectName = sdf.format(new Date()) + "/" + fileNewName;
            // 存储文件
            minioClient.putObject(BUCKETNAME, objectName, file.getInputStream(),
                    new PutObjectOptions(file.getInputStream().available(), -1));
            LOGGER.info("文件上传成功!");
            s=ENDPOINT + "/" + BUCKETNAME + "/" + objectName;
        } catch (Exception e) {
            LOGGER.info("上传发生错误: {}！", e.getMessage());
        }
        return s;
    }

    /**
     * 删除文件
     * @param name 具体文件名
     * @return 提示信息
     */
    @DeleteMapping("/demodelete")
    public String delete(String name) {
        try {
            MinioClient minioClient = new MinioClient(ENDPOINT, ACCESSKEY, SECRETKEY);
            minioClient.removeObject(BUCKETNAME, name);
        } catch (Exception e) {
            return "删除失败"+e.getMessage();
        }
        return "删除成功";
    }

    /**
     * 下载文件
     * @param filename 相对文件路径
     * @param httpResponse 响应
     */
    @GetMapping("/demodownload")
    public void downloadFiles(@RequestParam("filename") String filename, HttpServletResponse httpResponse) {

        try {
            String name = "";
            //文件名截取
            if(filename.lastIndexOf("/") != -1 ){
                name = filename.substring(filename.lastIndexOf("/") + 1);
            }else{
                name = filename;
            }
            MinioClient minioClient = new MinioClient(ENDPOINT, ACCESSKEY, SECRETKEY);
            InputStream object = minioClient.getObject(BUCKETNAME, filename);
            byte buf[] = new byte[1024];
            int length = 0;
            httpResponse.reset();

            httpResponse.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(name, "UTF-8"));
            httpResponse.setContentType("application/octet-stream");
            httpResponse.setCharacterEncoding("utf-8");
            OutputStream outputStream = httpResponse.getOutputStream();

            while ((length = object.read(buf)) > 0) {
                outputStream.write(buf, 0, length);
            }
            outputStream.close();
        } catch (Exception ex) {
            LOGGER.info("导出失败：", ex.getMessage());
        }
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
    @GetMapping("/demoPreviewList")
    public List<Object> getPreviewList() throws XmlParserException, NoSuchAlgorithmException, InsufficientDataException, InternalException, InvalidResponseException, InvalidKeyException, InvalidBucketNameException, ErrorResponseException, IOException, InvalidExpiresRangeException, InvalidPortException, InvalidEndpointException {
        MinioClient minioClient = new MinioClient(ENDPOINT, ACCESSKEY, SECRETKEY);
        Iterable<Result<Item>> myObjects = minioClient.listObjects(BUCKETNAME);
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
            String filePath = minioClient.presignedGetObject(BUCKETNAME, item.objectName());
            items.add(JSON.parse(String.format(format, filePath)));
        }
        return  items;
    }




}

