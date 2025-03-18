package com.openclassrooms.tourguide.service;

import java.util.*;
import java.util.concurrent.*;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;


@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	public void calculateRewards(User user) {

		ConcurrentHashMap<Location, VisitedLocation> userVisitedLocationMap = new ConcurrentHashMap<>();
		user.getVisitedLocations().forEach(visitedLocation ->
				userVisitedLocationMap.put(visitedLocation.location, visitedLocation)
		);

		ConcurrentHashMap<String, UserReward> userRewardsMap = new ConcurrentHashMap<>();
		user.getUserRewards().forEach(reward ->
				userRewardsMap.put(reward.attraction.attractionName, reward)
		);

		List<Attraction> attractions = gpsUtil.getAttractions();
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); //8 Pool de threads pour contrôler les ressources.

		CompletableFuture<ConcurrentHashMap<String, UserReward>> future = CompletableFuture.supplyAsync(() -> {
			ConcurrentHashMap<String, UserReward> chmUserRewardsMap = new ConcurrentHashMap<>(userRewardsMap);

			//userVisitedLocationMap.entrySet().forEach(entry -> {
			List<Map.Entry<Location, VisitedLocation>> entryList = new ArrayList<>(userVisitedLocationMap.entrySet());
			for (Map.Entry<Location, VisitedLocation> entry: entryList) {
				for (Attraction attraction : attractions) {
					if (!chmUserRewardsMap.containsKey(attraction.attractionName)) {
						if (nearAttraction(entry.getValue(), attraction)) {
							UserReward newReward = new UserReward(entry.getValue(), attraction, getRewardPoints(attraction, user));
							chmUserRewardsMap.put(newReward.attraction.attractionName, newReward);
						}
					}
				}
			}
			return chmUserRewardsMap;
		}, executor);

		future.thenAccept(chmUserRewardsMap -> {
			//System.out.println("Contenu du CHMUserRewardsMap : " + chmUserRewardsMap);
			chmUserRewardsMap.forEach((key, value) -> user.addUserReward(value));
		});

		future.exceptionally(ex -> {
			System.err.println("Task error : " + ex.getMessage());
			return null;
		});

		future.whenComplete((result, throwable) -> executor.shutdown());


	}
	
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		//return getDistance(attraction, location) > attractionProximityRange ? false : true;

		List<Attraction> attractionsList = new ArrayList<>(gpsUtil.getAttractions());
		List<Attraction> nearestFiveAttractions = new ArrayList<>();

		// Attractions sort by distance
		attractionsList.sort(Comparator.comparingDouble(attractionlocation ->
				getDistance(new Location(attractionlocation.latitude, attractionlocation.longitude), location)
		));

		// Get the five first locations
		nearestFiveAttractions = attractionsList.subList(0, Math.min(5, attractionsList.size()));

		return nearestFiveAttractions.stream().anyMatch(a -> a.attractionName.equals(attraction.attractionName));
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
	//return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
		return !(getDistance(attraction, visitedLocation.location) > proximityBuffer);
	}
	
	private int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

}
