package ru.morgan.exelparser.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.client.RestTemplate;
import ru.morgan.exelparser.models.*;
import ru.morgan.exelparser.models.map.GeocodeResponse;
import ru.morgan.exelparser.repositories.*;
import ru.morgan.exelparser.utils.CustomFileUtil;
import ru.morgan.exelparser.utils.StringGenerator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class DatabasePreConfiguration {
    @Bean
    public CommandLineRunner dataLoader(
            EkpRepository ekpRepository,
            SportRepository sportRepository,
            DisciplineRepository disciplineRepository,
            SportObjectRepository sportObjectRepository,
            SchoolRepository schoolRepository,
            MinioFileRepository minioRepository
    ){
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {
                /*MinioClient minioClient = MinioClient.builder()
                        .endpoint("http://45.95.234.119:9000")
                        .credentials("morgan", "Vjh911ufy!")
                        .build();*/
                //parseEkp(ekpRepository,sportRepository,disciplineRepository);
                //parserSportObject(sportObjectRepository, minioRepository, minioClient);
                //parseSchools(schoolRepository,minioRepository,minioClient);
                setSubjectForSchools(schoolRepository);
            }
        };
    }

    private void setSubjectForSchools(SchoolRepository schoolRepository){
        long count = schoolRepository.count();
        System.out.println("Заявлено: " + count);
        int counter = 0;
        for(long i=1; i<=count; i++){
            Optional<School> schoolOptional = schoolRepository.findById(i);
            if (schoolOptional.isPresent()){
                School school = schoolOptional.get();
                String[] part = school.getAddress().split(", ");
                school.setSubject(part[1]);
                schoolRepository.save(school);
                counter++;
            }
        }
        System.out.println("Реально найдено: " + counter);
    }

    private void parseSchools(SchoolRepository schoolRepository, MinioFileRepository minioFileRepository, MinioClient minioClient){
        System.out.println("start process");
        XSSFWorkbook wb = getWorkBookFromXSSF("./src/main/resources/static/file/schools.xlsx");
        XSSFSheet sheet = wb.getSheet("list");
        Iterator<Row> rowIter = sheet.rowIterator();

        int count = 0;
        while (rowIter.hasNext()) {
            Row row = rowIter.next();
            Cell OGRNCell = row.getCell(0);
            Cell schoolNameCell = row.getCell(1);
            Cell indexCell = row.getCell(2);
            Cell addressCell = row.getCell(3);
            Cell coordsCell = row.getCell(4);
            Cell urlCell = row.getCell(7);
            Cell imageCell = row.getCell(8);

            // 1 ЭТАП СОБИРАЕМ ОГРН
            String OGRN = null;
            if(OGRNCell != null){
                if(!OGRNCell.toString().equals("")){
                    DataFormatter dataFormatter = new DataFormatter();
                    OGRN = dataFormatter.formatCellValue(OGRNCell);
                }
            }

            // 2 ЭТАП СОБИРАЕМ ПОЛНОЕ НАЗВАНИЕ ШКОЛЫ
            String schoolName = null;
            if(schoolNameCell != null){
                if(!schoolNameCell.toString().equals("")){
                    schoolName = schoolNameCell.toString();
                }
            }

            // 3 ЭТАП СОБИРАЕМ ИНДЕКС
            String index = null;
            if(indexCell != null){
                if(!indexCell.toString().equals("")){
                    Pattern pattern = Pattern.compile("\\d{6}");
                    Matcher matcher = pattern.matcher(indexCell.toString());
                    if(matcher.find()) {
                        index = matcher.group();
                    }
                }
            }

            // 4 ЭТАП СБОР АДРЕСОВ
            String address = null;
            if(addressCell != null){
                if(!addressCell.toString().equals("")){
                    address = addressCell.toString();
                }
            }

            // 5 ЭТАП СБОР КООРДИНАТ
            String s = null;
            String d = null;
            if(coordsCell != null){
                if(!coordsCell.toString().equals("")){
                    String[] part = coordsCell.toString().split(", ");
                    if(part.length > 1) {
                        s = part[0];
                        d = part[1];
                    }
                }
            }

            // 6 СБЛО URL
            String url = null;
            if(urlCell != null){
                if(!urlCell.toString().equals("")){
                    url = urlCell.toString();
                }
            }

            // 7 Сбор изображений
            String logo = null;
            String photo = null;
            if(imageCell != null){
                if(!imageCell.toString().equals("")){
                    String[] part = imageCell.toString().split(", ");
                    if(part.length == 2){
                        logo = part[0];
                        photo = part[1];
                    }
                }
            }

            if(OGRN != null && schoolName != null && index != null && address != null && s != null && d != null && url != null && logo != null && photo != null){
                MinioResponse logoResponse = minioService(minioClient,"logo", logo);
                MinioResponse photoResponse = minioService(minioClient,"photo", photo);
                if(logoResponse != null && photoResponse != null){
                    MinioFile logoFile = new MinioFile(logoResponse);
                    MinioFile photoFile = new MinioFile(photoResponse);
                    logoFile = minioFileRepository.save(logoFile);
                    photoFile = minioFileRepository.save(photoFile);

                    School school = new School();
                    school.setOgrn(Long.parseLong(OGRN));
                    school.setName(schoolName);
                    school.setIndex(Integer.parseInt(index));
                    school.setAddress(address);
                    school.setS(Float.parseFloat(s));
                    school.setD(Float.parseFloat(d));
                    school.setUrl(url);
                    school.setLogoId(logoFile.getId());
                    school.setPhotoId(photoFile.getId());

                    school = schoolRepository.save(school);

                    System.out.println("Школа сохранена: " + school);

                    count++;
                }
            }
        }

        System.out.println("Всего записей: " + count);


        try {
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("end process");
    }

    private void parserSportObject(SportObjectRepository sportObjectRepository, MinioFileRepository minioRepository, MinioClient minioClient){
        System.out.println("start process");
        XSSFWorkbook wb = getWorkBookFromXSSF("./src/main/resources/static/file/sport-object.xlsx");
        XSSFSheet sheet = wb.getSheet("list");
        Iterator<Row> rowIter = sheet.rowIterator();

        int count = 0;
        List<SportObject> sportObjects = new ArrayList<>();
        while (rowIter.hasNext()) {
            count++;
            SportObject sportObject = new SportObject();
            Row row = rowIter.next();
            Cell locationCell = row.getCell(0);
            Cell addressCell = row.getCell(1);
            Cell titleCell = row.getCell(2);
            Cell registerDateCell = row.getCell(3);
            Cell coordsCell = row.getCell(4);
            Cell urlCell = row.getCell(5);
            Cell logoCell = row.getCell(6);
            Cell imageCell = row.getCell(7);

            if(logoCell != null){
                if(!logoCell.toString().equals("")){
                    if(imageCell != null){
                        if(!imageCell.toString().equals("")){
                            String[] image = imageCell.toString().split(", ");
                            List<String> images = new ArrayList<>(List.of(image));
                            sportObject.setLogo(logoCell.toString());
                            sportObject.setImages(images);
                        }
                    }
                }
            }

            String location = locationCell.toString();
            String address = addressCell.toString();
            String title = titleCell.toString();
            LocalDate register = getRegisterDate(registerDateCell.toString());

            String coords = "";
            if(coordsCell != null){
                if(!coordsCell.toString().equals("")){
                    coords = coordsCell.toString();
                    String[] part = coords.split(", ");
                    float s = Float.parseFloat(part[0]);
                    float d = Float.parseFloat(part[1]);
                    sportObject.setS(s);
                    sportObject.setD(d);
                }
            }

            sportObject.setTitle(title);
            sportObject.setLocation(location);
            sportObject.setAddress(address);
            sportObject.setRegisterDate(register);
            if(urlCell != null){
                if(!urlCell.toString().equals("")){
                    sportObject.setUrl(urlCell.toString());
                }
            }
            sportObjects.add(sportObject);
        }

        for(SportObject sportObject : sportObjects){

            minioService(minioClient,"logo", sportObject.getLogo());

            /*if(sportObject.getS() == 0.0f){
                try {
                    String[] part = sportObject.getAddress().split(", ");
                    String add = "";
                    for(int i=2; i<part.length; i++){
                        add += part[i];
                    }
                    GeoDate geoDate = getCoords(add);

                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }*/
        }

        try {
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("end process");
    }

    private GeoDate getCoords(String address) throws IOException, URISyntaxException {
        String apiKey = "AqvShZnoih8TXASgC7kM2gk-4FEylZMPa_S1E5TYsZu3SfH8czu80D86osszgrYo";
        String urlString = "https://dev.virtualearth.net/REST/v1/Locations?query=" + URLEncoder.encode(address, StandardCharsets.UTF_8) + "&key=" + apiKey;
        URI uri = new URI(urlString);
        URL url = uri.toURL();

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        String answer = response.toString();

        Pattern pattern = Pattern.compile("\"coordinates\":\\s*\\[(\\d+\\.\\d+),\\s*(\\d+\\.\\d+)\\]");
        Matcher matcher = pattern.matcher(answer);
        float s = 0,d = 0;
        if (matcher.find()) {
            String latitude = matcher.group(1);
            String longitude = matcher.group(2);
            s = Float.parseFloat(latitude);
            d = Float.parseFloat(longitude);
        }

        reader.close();

        GeoDate geoDate = new GeoDate();
        geoDate.setS(s);
        geoDate.setD(d);
        if(s == 0.0f){
            geoDate.setS(55.755863f);
            geoDate.setD(37.617700f);
        }

        return geoDate;
    }

    @Data
    class GeoDate{
        private float s;
        private float d;
    }

    private LocalDate getRegisterDate(String date){
        LocalDate localDate = LocalDate.now();
        if(!date.equals("")) {
            String[] part = date.split("-");
            DateTimeFormatter formatter;
            if (part.length > 1) {
                formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", new Locale("ru"));
            } else {
                formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            }
            localDate = LocalDate.parse(date, formatter);
        }
        return localDate;
    }

    private void parseEkp(EkpRepository ekpRepository, SportRepository sportRepository, DisciplineRepository disciplineRepository){
        System.out.println("start process");
        XSSFWorkbook wb = getWorkBookFromXSSF("./src/main/resources/static/file/ekp-3.xlsx");
        XSSFSheet sheet = wb.getSheet("list");
        Iterator<Row> rowIter = sheet.rowIterator();

        /*9a4e8022-c477-4474-8a6e-e117646f9c85 - ключ Димы +*/
        /*f5aede27-f6c6-4c8d-b65c-0b03e09357dc - domensport.ru* +/
        /*137ef6a7-52e7-45e6-8fb9-a04ffa009134 - Мой ключ + */
        /*7c13cf7d-c937-4ca6-8021-145ac16bdcae мой ключ */
        /*597949f0-988f-4938-8e44-e4c0aead5872 - Дима 2 +*/
        String apiKey = "7c13cf7d-c937-4ca6-8021-145ac16bdcae";
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

            if(2500 < count && count < 3500) {
                if (beginning != null && ending != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", new Locale("ru"));
                    LocalDate begin = LocalDate.now();
                    if(!beginning.toString().equals("")){
                        begin = LocalDate.parse(beginning.toString(), formatter);
                    }
                    LocalDate end = LocalDate.now();
                    if(!ending.toString().equals("")){
                        end = LocalDate.parse(ending.toString(), formatter);
                    }
                    Sport sport = sportRepository.findByTitleLikeIgnoreCase(sportName.toString());
                    if (sport != null) {
                        long sid = sport.getId();
                        Set<Long> dids = new HashSet<>();
                        if (discipline != null) {
                            String[] dis = discipline.toString().split(", ");
                            for (String di : dis) {
                                List<Discipline> disciplineList = disciplineRepository.findByTitleLikeIgnoreCase(di);
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

                                        Status status = parseStatus(statusName.toString());
                                        Ekp ekp = new Ekp();
                                        ekp.setEkp(ekpNum.toString());
                                        ekp.setTitle(title.toString());
                                        ekp.setDescription("У данного мероприятия нет описания");
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
                                        Ekp savedEkp = ekpRepository.save(ekp);
                                        System.out.println(savedEkp);
                                    }else{
                                        System.out.println("geo-response error - " + count);
                                    }
                                }else{
                                    System.out.println(count + " geo-null");
                                }
                            }else{
                                System.out.println("dids 0");
                            }
                        }else{
                            System.out.println("дисциплины ноль");
                        }
                    } else {
                        System.out.println(sportName.toString());
                    }
                }else{
                    System.out.println(count + "Проблема с датами ");
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

    public MinioResponse minioService(MinioClient minioClient,String sub, String name){
        String filePath = "./src/main/resources/static/img/" + sub + "/" + name;
        File file = new File(filePath);
        if(file.exists()){
            return uploadImage(file,minioClient);
        }
        return null;
    }

    public MinioResponse uploadImage(File file, MinioClient minioClient) {
        //Путь к временной директории
        String directory = "./src/main/resources/static/img/temp";
        //Случайное имя файла
        String tempName = StringGenerator.getGeneratedString(20);
        // Формируем UID
        String extension = "webp";
        String randomWord = StringGenerator.getGeneratedString(30);
        String uid = randomWord + "." + extension;

        try {
            String temp = directory + tempName + "." + extension;
            InputStream fileInputStream = new FileInputStream(file);
            BufferedImage bufferedImage = ImageIO.read(fileInputStream);
            boolean completed = ImageIO.write(bufferedImage, "webp", new File(temp));
            if (completed) {
                File webpImage = new File(temp);
                InputStream webpImageInputStream = new FileInputStream(webpImage);
                String type = "image/webp";
                PutObjectArgs args = PutObjectArgs.builder()
                        .bucket("upload")
                        .object("/" + type + "/" + uid)
                        .contentType(type)
                        .stream(webpImageInputStream, -1, 10485760)
                        .build();
                ObjectWriteResponse response = minioClient.putObject(args);
                MinioResponse minioResponse = new MinioResponse();
                minioResponse.setResponse(response);
                minioResponse.setOriginalFileName(file.getName());
                minioResponse.setUid(uid);
                minioResponse.setType(type);
                minioResponse.setFileSize(CustomFileUtil.getMegaBytes(fileInputStream.available()));
                fileInputStream.close();
                webpImageInputStream.close();
                cleanup(directory, tempName);
                return minioResponse;
            } else {
                return null;
            }
        } catch (ServerException | InsufficientDataException | ErrorResponseException |
                 NoSuchAlgorithmException | InvalidKeyException | InvalidResponseException |
                 XmlParserException | InternalException | IOException e) {
            throw  new RuntimeException(e);
        }
    }

    @SneakyThrows
    private void cleanup(String directory, String tempName) {
        File f1 = new File(directory + tempName);
        File f2 = new File(directory + tempName + ".webp");
        if (f1.exists()) {
            Files.delete(f1.toPath());
        }
        if (f2.exists()) {
            Files.delete(f2.toPath());
        }
    }
}
