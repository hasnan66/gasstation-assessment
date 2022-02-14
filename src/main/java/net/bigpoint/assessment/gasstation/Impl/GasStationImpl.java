package net.bigpoint.assessment.gasstation.Impl;

import net.bigpoint.assessment.gasstation.GasPump;
import net.bigpoint.assessment.gasstation.GasStation;
import net.bigpoint.assessment.gasstation.GasType;
import net.bigpoint.assessment.gasstation.exceptions.GasTooExpensiveException;
import net.bigpoint.assessment.gasstation.exceptions.NotEnoughGasException;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * The purpose of the document is to includes multiple gas pumps for different types of fuel
 * at different prices.
 */
public class GasStationImpl implements GasStation {

    /**
     * For logging details, Logger instance
     */
    private static final Logger LOG = Logger.getLogger(GasStationImpl.class.getName());

    /**
     * Collection of Gas Pumps this station has.
     */
    private CopyOnWriteArrayList<GasPump> gasPumps = new CopyOnWriteArrayList<GasPump>();

    /**
     * Map with list of gas types and corresponding prices.
     */
    private ConcurrentMap<GasType, Double> pricesMap = new ConcurrentHashMap<GasType, Double>();

    /**
     * Total revenue of the station.
     */
    private AtomicLong revenue = new AtomicLong(0);

    /**
     * Total sales of the station.
     */
    private AtomicInteger totalSale = new AtomicInteger(0);

    /**
     * Total transactions canceled because of no gas.
     */
    private AtomicInteger cancellationNoGasCount = new AtomicInteger(0);

    /**
     * Total transactions canceled because of too expensive gas.
     */
    private AtomicInteger cancellationTooExpensiveCount = new AtomicInteger(0);

    /**
     * Get List of Gas Pumps this station has.
     *
     * @return Collection of GasPumps
     */
    public Collection<GasPump> getGasPumps() {
        return this.gasPumps;
    }

    /**
     * Add a new GasPump to the List of pumps this station has.
     *
     * @param pump GasPump item
     */
    public void addGasPump(GasPump pump) {
        this.gasPumps.add(pump);
    }

    /**
     * Get total revenue of this station.
     *
     * @return total revenue
     */
    public double getRevenue() {
        return this.revenue.doubleValue();
    }

    /**
     * Get total amount of sales performed by the station.
     *
     * @return total sales
     */
    public int getNumberOfSales() {
        return this.totalSale.get();
    }

    /**
     * Get total number of operations canceled because of no gas.
     *
     * @return total canceled because of no gas
     */
    public int getNumberOfCancellationsNoGas() {
        return this.cancellationNoGasCount.get();
    }

    /**
     * Get total number of operations canceled because of too expensive
     *
     * @return total canceled because of too expensive
     */
    public int getNumberOfCancellationsTooExpensive() {
        return this.cancellationTooExpensiveCount.get();
    }

    /**
     * Get price for a specific gas type.
     *
     * @param type GasType
     * @return price
     */
    public double getPrice(GasType type) {
        return this.pricesMap.get(type);
    }

    /**
     * Set price for a given gas type
     *
     * @param type  GasType
     * @param price Price for gas type
     */
    public void setPrice(GasType type, double price) {
        this.pricesMap.put(type, price);
    }

    /**
     * Let a customer buy gas by type, specifying total amount of liters needed and max price per liter to pay.
     *
     * @param type             GasType
     * @param amountInLiters   Total liters needed
     * @param maxPricePerLiter Max price willing to pay per liter
     * @return total price for transaction
     * @throws NotEnoughGasException
     * @throws GasTooExpensiveException
     */
    public double buyGas(GasType type, double amountInLiters, double maxPricePerLiter) throws NotEnoughGasException, GasTooExpensiveException {

        double pricePerLiter = pricesMap.get(type);
        double price = 0;

        //Validate price per liter. If the current price is higher than maxPricePerLiter param, throw exception
        if (pricePerLiter > maxPricePerLiter) {
            cancellationTooExpensiveCount.addAndGet(1);
            throw new GasTooExpensiveException();
        }

        //Iterate through pumps
        for (GasPump gasPump : gasPumps) {
            //This pump serves the gas type requested
            if (gasPump.getGasType().equals(type)) {
                //Lock it while using so no other Thread has access
                synchronized (gasPump) {
                    //This pump has enough fuel to serve
                    if (gasPump.getRemainingAmount() >= amountInLiters) {
                        gasPump.pumpGas(amountInLiters);
                        price = amountInLiters * pricePerLiter;
                        LOG.info("[PUMP INFO] amount remaining: " + gasPump.getRemainingAmount());
                        revenue.addAndGet((long) price);
                        totalSale.addAndGet(1);
                        break;
                    }
                }
            }
        }

        //when gases not available, increment the counter and throw exception
        if (price == 0 && amountInLiters > 0) {
            cancellationNoGasCount.addAndGet(1);
            throw new NotEnoughGasException();
        }

        return price;
    }
}
