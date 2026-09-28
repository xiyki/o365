package hqr.o365.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private AuthenticationProvider provider;  //注入我们自己的AuthenticationProvider

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().ignoringAntMatchers("/callback")
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).and()
				.headers().frameOptions().sameOrigin().and();
		http
				// 登录处理
				.formLogin() // 表单方式，或httpBasic
				.loginPage("/loginPage").loginProcessingUrl("/login").defaultSuccessUrl("/home", true) // 成功登陆后跳转页面
				.failureUrl("/error/401.html").permitAll().and();
		http.authorizeRequests() // 授权配置
				// 无需权限访问
				.antMatchers("/h2/**","/*.png","/*.js", "/*.svg", "/modern.css", "/jquery-easyui-1.9.14/**", "/", "/index.html", "/loginPage","/reg","/reg.html",
						"/chkUserId","/callback","/refer","/refer.html","/createUserByInviteCd", "/*.jpg").permitAll()
				// 其他接口需要登录后才能访问
				.anyRequest().authenticated().and();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                //用户认证处理
                .authenticationProvider(provider);
    }
	
}
