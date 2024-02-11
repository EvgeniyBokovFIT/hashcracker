package com.example.worker;

import generated.CrackHashManagerRequest;
import generated.CrackHashWorkerResponse;
import generated.ObjectFactory;
import lombok.RequiredArgsConstructor;
import org.paukov.combinatorics3.Generator;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HashCrackWorkerService {
    private final RabbitTemplate template;

    public void processRequest(CrackHashManagerRequest request){

        List<String> foundWords = crackHash(request);
        ObjectFactory objectFactory = new ObjectFactory();
        CrackHashWorkerResponse.Answers answers = objectFactory.createCrackHashWorkerResponseAnswers();
        answers.getWords().addAll(foundWords);
        CrackHashWorkerResponse response = objectFactory.createCrackHashWorkerResponse();
        response.setAnswers(answers);
        response.setPartNumber(request.getPartNumber());
        response.setRequestId(request.getRequestId());

        template.convertAndSend("to_manager", response);
    }

    private static List<String> crackHash(CrackHashManagerRequest request) {
        int alphabetSize = request.getAlphabet().getSymbols().size();
        long partSize = (long) Math.ceil(
                Math.pow(alphabetSize, request.getMaxLength()) /
                        request.getPartCount());

        return Generator.permutation(request.getAlphabet().getSymbols())
                .withRepetitions(request.getMaxLength())
                .stream()
                .skip(partSize * request.getPartNumber())
                .limit(partSize)
                .filter(word -> request.getHash().equals(calculateMd5(String.join("", word))))
                .map(word -> String.join("", word))
                .toList();
    }

    private static String calculateMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
