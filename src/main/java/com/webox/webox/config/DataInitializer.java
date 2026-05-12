package com.webox.webox.config;
import com.webox.webox.entity.MenuItem;
import com.webox.webox.repository.MenuItemRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class DataInitializer implements ApplicationRunner {
    private final MenuItemRepository repo;
    public DataInitializer(MenuItemRepository repo) { this.repo = repo; }

    @Override
    public void run(ApplicationArguments args) {
        if (repo.count() > 0) return;
        save("item_001","宫保鸡丁","经典川菜,鸡肉搭配花生、干辣椒爆炒","22","chinese","/img/placeholder-kungpao.svg","peanut");
        save("item_002","凯撒沙拉","新鲜罗马生菜配帕玛森芝士与凯撒酱","28","salad","/img/placeholder-caesar.svg","dairy,egg");
        save("item_003","三文鱼刺身定食","新鲜三文鱼刺身搭配米饭、味噌汤","45","japanese","/img/placeholder-sashimi.svg","fish");
        save("item_004","番茄意面","经典意式番茄酱意大利面配新鲜罗勒","26","western","/img/placeholder-pasta.svg","gluten");
        save("item_005","冬阴功汤","泰式酸辣虾汤配香茅、南姜、柠檬叶","32","southeast_asian","/img/placeholder-tomyum.svg","shellfish");
        save("item_006","鸡胸肉藜麦碗","低脂高蛋白,烤鸡胸配藜麦、牛油果、时蔬","35","salad","/img/placeholder-quinoa.svg","");
        save("item_007","麻婆豆腐","四川经典,嫩豆腐配麻辣肉末","18","chinese","/img/placeholder-mapo.svg","soy");
        save("item_008","韩式拌饭","石锅拌饭配各式时蔬、煎蛋与辣酱","30","korean","/img/placeholder-bibimbap.svg","egg,soy");
    }

    private void save(String code,String name,String desc,String price,String cat,String img,String allergens){
        MenuItem m = new MenuItem();
        m.setCode(code); m.setName(name); m.setDescription(desc);
        m.setPrice(new BigDecimal(price)); m.setCategory(cat);
        m.setImage(img); m.setAllergens(allergens);
        repo.save(m);
    }
}
