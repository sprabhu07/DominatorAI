/*
 * GHOST (General meta-Heuristic Optimization Solving Tool) is a C++ library 
 * designed to help developers to model and implement optimization problem 
 * solving. It contains a meta-heuristic solver aiming to solve any kind of 
 * combinatorial and optimization real-time problems represented by a CSP/COP. 
 *
 * GHOST has been first developped to help making AI for the RTS game
 * StarCraft: Brood war, but can be used for any kind of applications where 
 * solving combinatorial and optimization problems within some tenth of 
 * milliseconds is needed. It is a generalization of the Wall-in project.
 * Please visit https://github.com/richoux/GHOST for further information.
 * 
 * Copyright (C) 2014-2017 Florian Richoux
 *
 * This file is part of GHOST.
 * GHOST is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as published 
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * GHOST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with GHOST. If not, see http://www.gnu.org/licenses/.
 */


#pragma once

#include <random>
#include <algorithm>

using namespace std;

namespace ghost
{
  //! Use this class to generate pseudo-random numbers following a near-uniform distribution. 
  /*!
   * Random uses the C++11 Mersenne Twister (mt19937) as a
   * pseudo-random generator. Seeds are generated by C++11 random_device.
   *
   * It follows a near-uniform distribution, but not a uniform one. This is to make 
   * the class more convenient to use by creating one Random object to get numbers within 
   * different ranges, instead of forcing creating one Random object for each specific range.
   * If you need to sample numbers from a purely uniform distribution, do not use this class.
   */
  class Random
  {
    random_device				_rd;
    mutable mt19937				_rng;
    mutable uniform_int_distribution<int>	_unif_dist;

    //! Only used for the copy-and-swap idiom.
    /*!
     * \sa operator=
     */
    void swap( Random &other )
    {
      std::swap( this->_rng, other._rng );
    }  

  public:
    //! Unique construtor.
    Random() : _rng( _rd() ) { }

    //! Unique copy constructor.
    Random( const Random &other )
      : _rng( _rd() )
    { }
    
    //! Copy assignment operator following the copy-and-swap idiom.
    Random& operator=( Random other )
    {
      this->swap( other );
      return *this;
    }

    //! Default destructor.
    ~Random() = default;

    /*!
     * Inline function returning a pseudo-random value from the range [0, limit[, 
     * according a near-uniform distribution, like discussed above.
     *
     * \param limit The upper bound of the range [0, limit[ from where a random value is computed.
     * \return A pseudo-random value in the range [0, limit[
     */
    inline int get_random_number( int limit ) const { return ( _unif_dist( _rng ) % limit ); }
  };
}
