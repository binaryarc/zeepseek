package com.zeepseek.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ZeepseekApplication {

	public static void main(String[] args) {

		// .env 파일 로드 (파일이 없으면 예외 발생할 수 있으니 옵션 설정도 가능)
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing() // 파일이 없으면 무시
				.load();

		dotenv.entries().forEach(entry -> System.out.println(entry.getKey() + "=" + entry.getValue()));

		// 예: OPENAI_API_KEY 값을 시스템 프로퍼티로 등록
		String openaiApiKey = dotenv.get("OPENAI_API_KEY");
		String mongodburi = dotenv.get("MONGODB_URI");

		if (openaiApiKey != null) {
			System.setProperty("OPENAI_API_KEY", openaiApiKey);
			System.setProperty("MONGODB_URI", mongodburi);
		}

		SpringApplication.run(ZeepseekApplication.class, args);
	}

}
