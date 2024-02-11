package com.example.manager;

import com.example.manager.dto.HashCrackRequest;
import com.example.manager.dto.HashCrackStatusResponse;
import com.example.manager.entity.RequestStatus;
import com.example.manager.entity.CrackEntity;
import com.example.manager.entity.UnsentTask;
import com.example.manager.repo.CrackEntityRepository;
import com.example.manager.repo.UnsentTaskRepository;
import generated.CrackHashManagerRequest;
import generated.CrackHashWorkerResponse;
import generated.ObjectFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class HashCrackManagerService {
    private static final List<String> ALPHABET = List.of("a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","0","1","2","3","4","5","6","7","8","9", "");
    private final int WORKERS_COUNT = 4;

    private final RabbitTemplate template;
    private final CrackEntityRepository crackEntityRepository;
    private final UnsentTaskRepository unsentTaskRepository;

    public String processRequest(HashCrackRequest request) {
        CrackEntity crackEntity = new CrackEntity();
        crackEntity.setRequestTime(LocalDateTime.now());
        crackEntityRepository.save(crackEntity);
        try {
            submitTask(crackEntity.getId(), request.getHash(), request.getMaxLength());
        } catch (AmqpException e){
            unsentTaskRepository.save(new UnsentTask(crackEntity.getId(), request.getHash(), request.getMaxLength()));
        }
        return crackEntity.getId();
    }

    @Scheduled(fixedDelay = 10000)
    private void checkResponses(){
        crackEntityRepository.findByRequestStatusAndRequestTimeBefore(RequestStatus.IN_PROGRESS,
                LocalDateTime.now().minusSeconds(600)).forEach((crackEntity) -> {
            crackEntity.setRequestStatus(RequestStatus.ERROR);
            crackEntityRepository.save(crackEntity);
        });
        try {
            unsentTaskRepository.findAll().forEach(unsentTask -> {
                submitTask(unsentTask.getId(), unsentTask.getHash(), unsentTask.getMaxLength());
                unsentTaskRepository.delete(unsentTask);
            });
        } catch (AmqpException ignored){
        }
    }

    private void submitTask(String requestId, String hash, int maxLength) throws AmqpException{
        ObjectFactory factory = new ObjectFactory();

        CrackHashManagerRequest.Alphabet alphabet = factory.createCrackHashManagerRequestAlphabet();
        alphabet.getSymbols().addAll(ALPHABET);

        CrackHashManagerRequest request = factory.createCrackHashManagerRequest();

        request.setRequestId(requestId);
        request.setHash(hash);
        request.setMaxLength(maxLength);
        request.setAlphabet(alphabet);
        request.setPartCount(WORKERS_COUNT);
        for (int i = 0; i < WORKERS_COUNT; i++) {
            request.setPartNumber(i);
            template.convertAndSend("to_worker", request);
        }
    }

    public HashCrackStatusResponse getStatus(String requestId) {
        CrackEntity crackEntity = crackEntityRepository.findById(requestId).orElse(null);
        if (crackEntity == null){
            return new HashCrackStatusResponse(RequestStatus.ERROR.name(), null);
        }
        RequestStatus requestStatus = crackEntity.getRequestStatus();
        if (requestStatus == RequestStatus.IN_PROGRESS) {
            return new HashCrackStatusResponse(RequestStatus.IN_PROGRESS.name(), null);
        }
        if (requestStatus == RequestStatus.READY) {
            return new HashCrackStatusResponse(RequestStatus.READY.name(), crackEntity.getResults());
        }

        return new HashCrackStatusResponse(RequestStatus.ERROR.name(), null);
    }

    public void updateRequestStatus(CrackHashWorkerResponse response) {
        CrackEntity crackEntity = crackEntityRepository.findById(response.getRequestId()).orElse(null);
        if(crackEntity == null){
            return;
        }
        crackEntity.setWorkersAnswered(crackEntity.getWorkersAnswered() + 1);
        if(crackEntity.getWorkersAnswered() == WORKERS_COUNT){
            crackEntity.setRequestStatus(RequestStatus.READY);
        }
        crackEntity.getResults().addAll(response.getAnswers().getWords());
        crackEntityRepository.save(crackEntity);
    }
}
