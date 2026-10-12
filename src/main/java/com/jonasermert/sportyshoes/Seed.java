package com.jonasermert.sportyshoes;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
@Component class Seed implements CommandLineRunner {
 private final Accounts accounts;private final Products products;private final PasswordEncoder encoder;
 @Value("${sporty.admin.password}") private String password;
 Seed(Accounts a,Products p,PasswordEncoder e){accounts=a;products=p;encoder=e;}
 public void run(String...args){if(accounts.findByEmail("admin@sportyshoes.local").isEmpty()){Account a=new Account();a.email="admin@sportyshoes.local";a.name="Administrator";a.role="ADMIN";a.password=encoder.encode(password);accounts.save(a);}
 if(products.count()==0){String[] names={"Velocity Runner","Court Classic","Trail Explorer"};String[] categories={"Running","Training","Outdoor"};for(int i=0;i<3;i++){Product p=new Product();p.name=names[i];p.category=categories[i];p.description="Comfort and support for your next personal best.";p.price=new BigDecimal(89+i*20+".00");p.stock=20;products.save(p);}}}
}
