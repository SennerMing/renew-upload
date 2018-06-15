package com.zw.renewupload;

import com.github.tobato.fastdfs.FdfsClientConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.support.RegistrationPolicy;


@ServletComponentScan
@SpringBootApplication(exclude = JpaRepositoriesAutoConfiguration.class)
//导入FdfsClient
@Import(FdfsClientConfig.class)
//解决jmx注册冲突
@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
public class RenewUploadApplication {

	public static void main(String[] args) {
		SpringApplication.run(RenewUploadApplication.class, args);
	}
}
