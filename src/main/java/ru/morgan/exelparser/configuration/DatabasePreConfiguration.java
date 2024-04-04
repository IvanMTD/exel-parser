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
import org.springframework.web.client.RestTemplate;
import ru.morgan.exelparser.models.*;
import ru.morgan.exelparser.models.map.GeocodeResponse;
import ru.morgan.exelparser.repositories.*;
import ru.morgan.exelparser.utils.StringGenerator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
            MinioFileRepository minioRepository
    ){
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {
                MinioClient minioClient = MinioClient.builder()
                        .endpoint("http://45.95.234.119:9000")
                        .credentials("morgan", "Vjh911ufy!")
                        .build();
                //parseEkp(ekpRepository,sportRepository,disciplineRepository);
                parserSportObject(sportObjectRepository, minioRepository, minioClient);
            }
        };
    }

    private void parserSportObject(SportObjectRepository sportObjectRepository, MinioFileRepository minioFileRepository, MinioClient minioClient){
        System.out.println("start process");
        XSSFWorkbook wb = getWorkBookFromXSSF("./src/main/resources/static/file/sport-object.xlsx");
        XSSFSheet sheet = wb.getSheet("list");
        Iterator<Row> rowIter = sheet.rowIterator();

        int count = 0;
        List<SportObject> sportObjects = new ArrayList<>();
        while (rowIter.hasNext()) {
            if(count >= 0) {
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

                if (logoCell != null) {
                    if (!logoCell.toString().equals("")) {
                        if (imageCell != null) {
                            if (!imageCell.toString().equals("")) {
                                String[] image = imageCell.toString().split(", ");
                                List<String> images = new ArrayList<>(List.of(image));
                                sportObject.setLogo(logoCell.toString());
                                sportObject.setImages(images);

                                String location = locationCell.toString();
                                String address = addressCell.toString();
                                String title = titleCell.toString();
                                LocalDate register = getRegisterDate(registerDateCell.toString());

                                String coords = "";
                                if (coordsCell != null) {
                                    if (!coordsCell.toString().equals("")) {
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
                                if (urlCell != null) {
                                    if (!urlCell.toString().equals("")) {
                                        sportObject.setUrl(urlCell.toString());
                                    }
                                }
                                sportObjects.add(sportObject);
                            }
                        }
                    }
                }
            }
            count++;
        }

        for(SportObject sportObject : sportObjects){
            if(sportObject.getLogo() != null){
                MinioResponse logoResponse = minioService(minioClient,"logo", sportObject.getLogo());
                if(logoResponse != null) {
                    long logoId = minioSave(logoResponse, minioFileRepository).getId();
                    Set<Long> imageIds = new HashSet<>();
                    for (String image : sportObject.getImages()) {
                        MinioResponse imageResponse = minioService(minioClient, "foto", image);
                        if(imageResponse != null) {
                            long id = minioSave(imageResponse, minioFileRepository).getId();
                            imageIds.add(id);
                        }
                    }
                    sportObject.setLogoId(logoId);
                    sportObject.setImageIds(imageIds);
                }
            }


            if(sportObject.getS() == 0.0f){
                try {
                    String[] part = sportObject.getAddress().split(", ");
                    String add = "";
                    for(int i=2; i<part.length; i++){
                        add += part[i];
                    }
                    GeoDate geoDate = getCoords(add);
                    System.out.println(geoDate);
                    sportObject.setS(geoDate.getS());
                    sportObject.setD(geoDate.getD());
                } catch (IOException | URISyntaxException e) {
                    System.out.println(e);
                }
            }

            sportObjectRepository.save(sportObject);
        }

        try {
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("end process");
    }

    private MinioFile minioSave(MinioResponse response, MinioFileRepository repository){
        if(response.getResponse() != null) {
            MinioFile minioFile = new MinioFile();
            minioFile.setUid(response.getUid());
            minioFile.setName(response.getOriginalFileName());
            minioFile.setType(response.getType());
            minioFile.setETag(response.getResponse().etag());
            minioFile.setBucket(response.getResponse().bucket());
            minioFile.setPath(response.getResponse().object());
            minioFile.setMinioUrl(response.getResponse().region() != null ? response.getResponse().region() : "no url");
            minioFile.setFileSize(response.getFileSize());
            return repository.save(minioFile);
        }else{
            return new MinioFile();
        }
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
            if(latitude.length() <= 11) {
                s = Float.parseFloat(latitude);
                d = Float.parseFloat(longitude);
            }
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

    public MinioResponse minioService(MinioClient minioClient,String sub, String name){
        String bucket = "upload";
        String directory = "./src/main/resources/static/temp/";
        String filePath = "./src/main/resources/static/" + sub + "/" + name;
        File file = new File(filePath);
        MinioResponse minioResponse = new MinioResponse();
        if(file.exists()){
            System.out.println("file " + file.getName() + " exist");
            String extension = "webp";
            String randomWord = StringGenerator.getGeneratedString(30);
            String uid = randomWord + "." + extension;
            String tempName = StringGenerator.getGeneratedString(20);
            try {
                System.out.println("try upload " + tempName);
                String temp = directory + tempName + "." + extension;
                InputStream fileInputStream = new FileInputStream(file);
                BufferedImage bufferedImage = ImageIO.read(fileInputStream);
                boolean completed = ImageIO.write(bufferedImage, "webp", new File(temp));
                if (completed) {
                    System.out.println("completed! do next!");
                    File webpImage = new File(temp);
                    InputStream webpImageInputStream = new FileInputStream(webpImage);
                    String type = "image/webp";
                    PutObjectArgs args = PutObjectArgs.builder()
                            .bucket(bucket)
                            .object("/" + type + "/" + uid)
                            .contentType(type)
                            .stream(webpImageInputStream, -1, 10485760)
                            .build();
                    ObjectWriteResponse response = minioClient.putObject(args);
                    minioResponse.setResponse(response);
                    minioResponse.setOriginalFileName(file.getName());
                    minioResponse.setUid(uid);
                    minioResponse.setType(type);
                    minioResponse.setFileSize((float)fileInputStream.available() / 1048576.0f);
                    fileInputStream.close();
                    webpImageInputStream.close();
                    cleanup(directory, tempName);
                    return minioResponse;
                }
            } catch (ServerException | InsufficientDataException | ErrorResponseException |
                     NoSuchAlgorithmException | InvalidKeyException | InvalidResponseException |
                     XmlParserException | InternalException | IOException e) {
                throw  new RuntimeException(e);
            }
        }
        return null;
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
