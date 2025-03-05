package com.openclassrooms.tourguide.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import static com.google.common.collect.Iterables.contains;

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

	@Async("taskExecutor")
	public void calculateRewards(User user) {

		Map<Location, VisitedLocation> userVisitedLocationMap = new ConcurrentHashMap<>();

		for (VisitedLocation userVisitedLocation : user.getVisitedLocations()) {
			userVisitedLocationMap.put(userVisitedLocation.location, userVisitedLocation);
		}

		//List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();

		// Use ConcurrentHashMap to avoid ConcurrentModificationException and make better the performances
		Map<String, UserReward> userRewardsMap = new ConcurrentHashMap<>();

		for (UserReward reward : user.getUserRewards()) {
			userRewardsMap.put(reward.attraction.attractionName, reward);
		}

		for (Map.Entry<Location, VisitedLocation> entry: userVisitedLocationMap.entrySet()) {

	//VisitedLocation visitedLocation : userLocations) {
			for (Attraction attraction : attractions) {
				if (!userRewardsMap.containsKey(attraction.attractionName)) {
					if (nearAttraction(entry.getValue(), attraction)) {
						UserReward newReward = new UserReward(entry.getValue(), attraction, getRewardPoints(attraction, user));
						userRewardsMap.put(newReward.attraction.attractionName, newReward);
					}
				}
			}
		}

		// Wait all the threads are ended
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}


		// Update the user rewards
		/*for (Map.Entry<String, UserReward> entry: userRewardsMap.entrySet()){
			user.addUserReward(entry.getValue());
		}*/
		userRewardsMap.forEach((key, value) -> user.addUserReward(value));

		/*List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();

		for(VisitedLocation visitedLocation : userLocations) {
			for(Attraction attraction : attractions) {
				if(user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
					if(nearAttraction(visitedLocation, attraction)) {
						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
					}
				}
			}
		}*/
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
