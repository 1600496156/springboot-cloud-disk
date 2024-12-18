package com.mhc.springbootclouddisk.common.constants;

public class Constants {
    public static final Integer LENGTH_4 = 4;
    public static final Integer LENGTH_5 = 5;
    public static final Integer LENGTH_10= 10;
    public static final Integer LENGTH_30= 30;
    public static final String CHECK_CODE = "check_code";
    public static final String CHECK_CODE_EMAIL = "check_code_email";
    public static final String REDIS_KEY_SEND_EMAIL_CODE = "email_code";
    public static final String REDIS_KEY_UPLOAD_USE_SPACE = "upload_use_space";
    public static final String REDIS_KEY_DELETE_FILE_USE_SPACE = "delete_file_use_space";
    public static final String REDIS_KEY_CREATE_DOWNLOAD_URL_DTO = "create_download_url_dto";
    public static final String REDIS_CHUNK_SIZES = "redis_chunk_sizes";
    public static final String REDIS_CHUNK_SAVE_SHARE_SIZES = "redis_chunk_save_share_sizes";
    public static final String REDIS_KEY_SEND_EMAIL_CODE_DTO = "redis_key_send_email_code_dto";
    public static final String REDIS_KEY_ADMIN_DELETE_USER_FILE_SPACE = "redis_key_admin_delete_user_file_space";


    public static final String CATEGORY_ALL = "all";
    public static final String CATEGORY_VIDEO = "video";
    public static final String CATEGORY_MUSIC = "music";
    public static final String CATEGORY_IMAGE = "image";
    public static final String CATEGORY_DOC = "doc";
    public static final String CATEGORY_OTHERS = "others";

    public static final Short FILE_STATUS_TRANSCODING = 0;
    public static final Short FILE_STATUS_TRANSCODING_FAILED = 1;
    public static final Short FILE_STATUS_TRANSCODING_SUCCESS = 2;

    public static final String FILE_UPLOADING = "uploading";
    public static final String FILE_UPLOAD_FINISH = "upload_finish";
    public static final String FILE_UPLOAD_SECONDS = "upload_seconds";

    public static final String FILE_TYPE_VIDEO = ".mp4";
    public static final String FILE_TYPE_MUSIC = ".mp3";
    public static final String FILE_TYPE_PICTURE_JPG = ".jpg";
    public static final String FILE_TYPE_PICTURE_PNG = ".png";
    public static final String FILE_TYPE_PICTURE_JPEG = ".jpeg";
    public static final String FILE_TYPE_PDF = ".pdf";
    public static final String FILE_TYPE_DOC = ".doc";
    public static final String FILE_TYPE_EXCEL = ".excel";
    public static final String FILE_TYPE_TXT = ".txt";
    public static final String FILE_TYPE_ZIP = ".zip";

    public static final String FILE_TYPE_M3U8 = ".m3u8";

    public static final String CHUNKS_SIZES = "chunks_sizes";
}
