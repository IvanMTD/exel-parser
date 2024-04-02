package ru.morgan.exelparser.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.morgan.exelparser.models.*;
import ru.morgan.exelparser.models.map.GeocodeResponse;
import ru.morgan.exelparser.repositories.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Configuration
public class DatabasePreConfiguration {
    @Bean
    public CommandLineRunner dataLoader(EkpRepository ekpRepository, SportRepository sportRepository, DisciplineRepository disciplineRepository){
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {
                parseEkp(ekpRepository,sportRepository,disciplineRepository);
            }
        };
    }

    private void parseEkp(EkpRepository ekpRepository, SportRepository sportRepository, DisciplineRepository disciplineRepository){
        System.out.println("start process");
        XSSFWorkbook wb = getWorkBookFromXSSF("./src/main/resources/static/file/ekp-2.xlsx");
        XSSFSheet sheet = wb.getSheet("list");
        Iterator<Row> rowIter = sheet.rowIterator();

        String apiKey = "9a4e8022-c477-4474-8a6e-e117646f9c85";
        String url = "https://geocode-maps.yandex.ru/1.x";
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        RestTemplate restTemplate = new RestTemplate();

        int count = 0;
        while (rowIter.hasNext()){
            Row row = rowIter.next();
            Cell ekpNum = row.getCell(0);
            Cell statusName = row.getCell(1);
            Cell title = row.getCell(2);
            Cell sportName = row.getCell(3);
            Cell discipline = row.getCell(4);
            Cell groups = row.getCell(5);
            Cell beginning = row.getCell(6);
            Cell ending = row.getCell(7);
            Cell address = row.getCell(8);

            if(count >= 0) {
                GeocodeResponse geocodeResponse = null;

                if (address != null) {
                    if(!address.toString().equals("")) {
                        try {
                            String urlWithParams = url + "?apikey=" + apiKey + "&geocode=" + address.toString() + "&format=json";
                            ResponseEntity<String> response = restTemplate.getForEntity(urlWithParams, String.class);
                            geocodeResponse = objectMapper.readValue(response.getBody(), GeocodeResponse.class);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                if (geocodeResponse != null) {
                    if (geocodeResponse.getResponse().getGeoObjectCollection().getFeatureMember().size() != 0) {
                        String[] location = geocodeResponse.getResponse().getGeoObjectCollection().getFeatureMember().get(0).getGeoObject().getPoint().getPos().split(" ");
                        float s = Float.parseFloat(location[1]);
                        float d = Float.parseFloat(location[0]);
                        if (beginning != null && ending != null) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", new Locale("ru"));
                            LocalDate begin = LocalDate.parse(beginning.toString(), formatter);
                            LocalDate end = LocalDate.parse(ending.toString(), formatter);
                            Sport sport = sportRepository.findByTitleLikeIgnoreCase(sportName.toString());
                            long sid = sport.getId();
                            Set<Long> dids = new HashSet<>();
                            if (discipline != null) {
                                String[] dis = discipline.toString().split(", ");
                                for (int i = 0; i < dis.length; i++) {
                                    List<Discipline> disciplineList = disciplineRepository.findByTitleLikeIgnoreCase(dis[i]);
                                    if (disciplineList != null) {
                                        for (Discipline discipline2 : disciplineList) {
                                            if (discipline2.getSportId() == sport.getId()) {
                                                long did = discipline2.getId();
                                                dids.add(did);
                                            }
                                        }
                                    }
                                }
                                if (dids.size() != 0) {
                                    Status status = parseStatus(statusName.toString());
                                    Ekp ekp = new Ekp();
                                    ekp.setEkp(ekpNum.toString());
                                    ekp.setTitle(title.toString());
                                    ekp.setDescription("У данного мероприятия нету описания");
                                    ekp.setLocation(address.toString());
                                    ekp.setOrganization("Министерство спорта РФ");
                                    ekp.setBeginning(begin);
                                    ekp.setEnding(end);
                                    ekp.setSportId(sid);
                                    ekp.setDisciplineIds(dids);
                                    ekp.setS(s);
                                    ekp.setD(d);
                                    ekp.setCategory(groups.toString());
                                    ekp.setStatus(status);
                                    ekpRepository.save(ekp);
                                }
                            }
                        }
                    }
                }
            }
            count++;
        }

        try {
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("end process");
    }

    private Status parseStatus(String status){
        if(status.equals(Status.CR.getTitle())){
            return Status.CR;
        }else if(status.equals(Status.MS.getTitle())){
            return Status.MS;
        }else if(status.equals(Status.VS.getTitle())){
            return Status.VS;
        }else if(status.equals(Status.KR.getTitle())){
            return Status.KR;
        }else if(status.equals(Status.PR.getTitle())){
            return Status.PR;
        }else{
            return Status.MSS;
        }
    }

    private XSSFWorkbook getWorkBookFromXSSF(String filePath){
        try{
            return new XSSFWorkbook(new FileInputStream(filePath));
        }catch (Exception e){
            System.out.println(e.getMessage());
            return null;
        }
    }

    private HSSFWorkbook getWorkBookFromHSSF(String filePath){
        try{
            return new HSSFWorkbook(new FileInputStream(filePath));
        }catch (Exception e){
            System.out.println(e.getMessage());
            return null;
        }
    }

    private String stringStandard(String word){
        String result = "";
        if(!word.equals("")) {
            result = word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
        }
        return result;
    }

    private LocalDate parseLocalDate(String dateString){

        // Это добавляет еще 8к записей ... но без дня и месяца
        /*if (Pattern.matches("^\\d{4}( г\\.)?$", dateString) || Pattern.matches("^\\d{4}(\\.0)?$", dateString)) {
            dateString = "01.01." + dateString.substring(0,4);
        }*/

        DateTimeFormatter formatter1  = DateTimeFormatter.ofPattern("dd.MM.yyyy г.");
        DateTimeFormatter formatter2  = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter formatter3  = DateTimeFormatter.ofPattern("dd-MMM-yyyy", new Locale("ru"));

        LocalDate date1 = null;
        LocalDate date2 = null;
        LocalDate date3 = null;

        try {
            date1 = LocalDate.parse(dateString,formatter1);
        }catch (Exception e){
            //System.out.println(e);
        }
        try {
            date2 = LocalDate.parse(dateString,formatter2);
        }catch (Exception e){
            //System.out.println(e);
        }
        try {
            date3 = LocalDate.parse(dateString,formatter3);
        }catch (Exception e){
            //System.out.println(e);
        }

        if(date1 != null) {
            return date1;
        }else if (date2 != null){
            return date2;
        }else if (date3 != null){
            return date3;
        }else{
            return null;
        }
    }

    private Category parseCategory(String categoryTitle){
        Category category = Category.BR;

        if(categoryTitle.contains("1")){
            if(categoryTitle.contains("р") || categoryTitle.contains("Р")){
                if(!categoryTitle.contains("ю")) {
                    if(!categoryTitle.contains("\n") && categoryTitle.length() < 9) {
                        category = Category.R1;
                    }
                }else{
                    if(!categoryTitle.contains("\n") && !categoryTitle.contains("   ")) {
                        category = Category.YN1;
                    }
                }
            }
        }

        if(categoryTitle.contains("2")){
            if(categoryTitle.contains("р") || categoryTitle.contains("Р")){
                if(!categoryTitle.contains("ю")) {
                    if(!categoryTitle.contains("\n") && categoryTitle.length() < 9) {
                        category = Category.R2;
                    }
                }else{
                    if(!categoryTitle.contains("\n") && !categoryTitle.contains("   ")) {
                        category = Category.YN2;
                    }
                }
            }
        }

        if(categoryTitle.contains("3")){
            if(categoryTitle.contains("р") || categoryTitle.contains("Р")){
                if(!categoryTitle.contains("ю")) {
                    if(!categoryTitle.contains("\n") && categoryTitle.length() < 9) {
                        category = Category.R3;
                    }
                }else{
                    if(!categoryTitle.contains("\n") && !categoryTitle.contains("   ")) {
                        category = Category.YN3;
                    }
                }
            }
        }

        if(categoryTitle.equals("КМС")){
            category = Category.KMS;
        }

        if(categoryTitle.equals("МС")){
            category = Category.MS;
        }

        if(categoryTitle.equals("ЗМС")){
            category = Category.ZMS;
        }

        if(categoryTitle.equals("МСМК")){
            category = Category.MSMK;
        }

        return category;
    }

    private Season parseSeason(String season){
        return switch (season) {
            case "Летний" -> Season.SUMMER;
            case "Зимний" -> Season.WINTER;
            default -> Season.ALL;
        };
    }

    private SportFilterType parseFilter(String filer){
        return switch (filer) {
            case "олимпийский" -> SportFilterType.OLYMPIC;
            case "неолимпийский" -> SportFilterType.NO_OLYMPIC;
            default -> SportFilterType.ADAPTIVE;
        };
    }

    private FederalDistrict parseDistrict(String district){
        return switch (district) {
            case "ЦФО" -> FederalDistrict.CFO;
            case "СЗФО" -> FederalDistrict.SZFO;
            case "ЮФО" -> FederalDistrict.YFO;
            case "СКФО" -> FederalDistrict.SKFO;
            case "ПФО" -> FederalDistrict.PFO;
            case "УФО" -> FederalDistrict.UFO;
            case "СФО" -> FederalDistrict.SFO;
            case "ДФО" -> FederalDistrict.DFO;
            default -> null;
        };
    }
}
